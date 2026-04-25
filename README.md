# ArcadiaTweaks

Umbrella mod-patch for the **Arcadia** modpack. Server-side, non-invasive, Mixin-based optimizations and tweaks for NeoForge **1.21.1** (build 21.1.221+).

> **First module shipped:** `botany` — server-side hot-path optimizations for [BotanyPots](https://github.com/Darkhax-Minecraft/BotanyPots) (Crop/Soil match memoization, hopper-pot export backoff, growth-tick coalescing, etc.).

## Design principles

1. **Mixin only.** No reflection-based patching, no class transformers, no AccessTransformers beyond what NeoForge already provides. The mod is a stack of `@Inject` / `@Redirect` / `@WrapOperation` Mixins.
2. **Non-invasive.** No public API surface change in the targeted mods, no recipe / datapack / registry mutation (KubeJS handles that side of the pack), no client-side rendering changes by default.
3. **Modular.** Each optimization domain lives behind a feature module (`botany`, future modules…). Each module can be disabled at runtime via the config file.
4. **Per-strategy kill-switch.** Inside a module, **every** Mixin path has a dedicated TOML toggle. Anything can be turned off without rebuilding.
5. **Addon-friendly.** Mixins use default priority (1000) and target stable internal classes; we never patch SPI / API surfaces consumed by addons (e.g. BotanyTrees).

## Project layout

Single Gradle project. Source code is split at the **package** level into `common/` (loader-agnostic logic) and `neoforge/` (entry point + Mixins). This keeps the door open for an Architectury split later if Fabric becomes a target, without paying the build complexity now.

```
ArcadiaTweaks/
├── build.gradle, settings.gradle, gradle.properties
├── src/main/
│   ├── java/com/teamarcadia/arcadiatweaks/
│   │   ├── ArcadiaTweaks.java                   # MOD_ID, LOGGER
│   │   ├── common/
│   │   │   ├── config/                          # ModConfigSpec root + per-module specs
│   │   │   ├── module/                          # ArcadiaModule SPI + ModuleRegistry
│   │   │   └── modules/
│   │   │       └── botany/                      # BotanyModule + BotanyConfig (kill-switches)
│   │   └── neoforge/
│   │       ├── ArcadiaTweaksNeoForge.java       # @Mod entry point
│   │       └── mixin/                           # all Mixin classes live here
│   └── resources/
│       ├── arcadiatweaks.mixins.json            # Mixin config (server-side only)
│       ├── pack.mcmeta
│       └── META-INF/neoforge.mods.toml
└── README.md
```

### Adding a new module

1. Create `common/modules/<id>/<Id>Module.java` implementing `ArcadiaModule`.
2. Create `common/modules/<id>/<Id>Config.java` carrying the ModConfigSpec entries (build them in the constructor, push/pop a `[<id>]` section).
3. Wire it into `ArcadiaConfig` (instantiate the config + add a `MODULE_<ID>_ENABLED` toggle under `[modules]`).
4. Register the module in `ModuleRegistry.bootstrap()`.
5. Drop your Mixin classes under `neoforge/mixin/<id>/` and list them in `arcadiatweaks.mixins.json`.

## Build

Requires **JDK 21** and the Gradle wrapper.

```bash
# first time only - generate the wrapper jar
gradle wrapper --gradle-version 8.10

# build
./gradlew build

# the jar lands in build/libs/arcadiatweaks-<version>.jar
```

### Dev runtime mods (`libs/`)

ArcadiaTweaks targets BotanyPots via Mixin, so dev runs need the **exact** jars from production on the classpath. We do not consume them from a public maven; we drop the jars from the production instance directly into `libs/`.

Source path on the maintainer's machine:

```
C:\Users\curveo\curseforge\minecraft\Instances\Arcadia Echoes Of Power V2\mods
```

Required jars (and the version each must match in `gradle.properties`):

| File in `libs/`                                | gradle.properties key  |
|------------------------------------------------|------------------------|
| `botanypots-neoforge-1.21.1-<ver>.jar`         | `botanypots_version`   |
| `bookshelf-neoforge-1.21.1-<ver>.jar`          | `bookshelf_version`    |
| `prickle-neoforge-1.21.1-<ver>.jar`            | `prickle_version`      |

`libs/*.jar` is gitignored - each developer copies their own from the production pack. When the pack updates BotanyPots/Bookshelf/Prickle, refresh `libs/` and bump the corresponding `gradle.properties` values to match the new filenames.

### Run a dev client / server

```bash
./gradlew runClient
./gradlew runServer
```

### Run automated tests

The `test` branch carries a NeoForge GameTest scaffolding (work in progress). On `main`, no automated tests yet.

The dev runs are pre-configured with `mixin.debug=true` and `mixin.debug.export=true`, so the bytecode of every Mixin-applied class is dumped to `run/.mixin.out/class/` for inspection.

## Install (production)

Drop the jar into your server `mods/` folder. ArcadiaTweaks declares **BotanyPots as `optional`** — if BotanyPots is absent the `botany` Mixins simply do not match anything and stay dormant.

The config file is generated on first run at:

```
<server>/config/arcadiatweaks-common.toml
```

Edit it, restart the server, done.

## Validate the Mixin pipeline

On server start, look for this line in the log:

```
[ArcadiaTweaks/INFO]: [Mixin] Pipeline OK - HelloWorldMixin attached to MinecraftServer.runServer.
```

If you see it, the Mixin transform is working and you can ship real optimizations on top.

## Benchmark protocol (Spark)

Reference: [`_bmad-output/planning-artifacts/botanypots-server-optimization-research-2026-04-25.md`](../_bmad-output/planning-artifacts/botanypots-server-optimization-research-2026-04-25.md), section 5.

### Required mods on the bench server

- [Spark](https://spark.lucko.me/) — sampler + tick monitor
- BotanyPots + the addons present in production (BotanyTrees, etc.)
- ArcadiaTweaks (the build to evaluate)

### Reproducible test world

- 30 chunks `entity-ticking` around spawn, force-loaded (`/forceload add ...`)
- 400 pots: 200 `BASIC` + 200 `HOPPER`
- Crop mix: 50% simple crop (wheat), 50% crop with `pot_predicate` (worst case for the matches() hot path)
- Hopper inventories: 100 pots over an empty chest, 100 pots over a **full** chest (worst case for S3)
- 1 fake player (`/player`) to keep the chunks loaded

### Measurement

```text
# Baseline (ArcadiaTweaks botany module disabled or jar absent):
warm up 2 minutes, then:
/spark profiler --timeout 180 --thread "Server thread"
/spark tickmonitor 60

# Run with module enabled, repeat the same commands.
# Compare flame graphs in the Spark web viewer.
```

Targets: **−80% cumulative time on `BotanyPotBlockEntity#tickPot`**, MSPT cut by at least 30% on the dense farm scenario. See research doc §5.3 / §5.5 for per-strategy go/no-go thresholds.

### Going live without surprises

Ramp the kill-switches one at a time:

1. Boot with everything **off** in `[botany]` — confirm the module loads cleanly and the hello-world Mixin still logs.
2. Enable `s1_matches_cache_enabled` only. Bench, validate.
3. Enable `s3_hopper_backoff_enabled`. Bench, validate.
4. Enable `s2_tick_coalescing_enabled` with `tick_coalescing_n=2`, then `=4`. Bench at each step.
5. Enable `a1_required_growth_ticks_cache_enabled`, `a2_light_flag_downgrade_enabled` if the profile still shows headroom.

Keep `safety_revalidate_period_ticks = 200` while ramping — it bounds any cache-staleness bug to ~10 seconds of weirdness max.

## License

MIT — see [LICENSE](LICENSE).

## Credits

- **Darkhax** — author of BotanyPots, the mod we're optimizing.
- **NeoForged** team — modding platform.
- **SpongePowered** — Mixin framework.
