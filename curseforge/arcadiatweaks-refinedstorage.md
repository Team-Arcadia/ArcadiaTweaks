# ArcadiaTweaks Refined Storage Patch - CurseForge publication

## Recommendation

Publish this as a separate file under the main **ArcadiaTweaks** CurseForge project. Avoid a separate page unless you clearly warn users not to install multiple ArcadiaTweaks variants together.

All variants use the same mod id: `arcadiatweaks`.

## File metadata

| Field | Value |
|---|---|
| File | `arcadiatweaks-0.1.1-refinedstorage.jar` |
| Version | `0.1.1-refinedstorage` |
| Release type | Beta |
| Minecraft | `1.21.1` |
| Loader | NeoForge |
| Side | Server-side only |
| License | MIT |

## CurseForge relations

| Mod | Relation | Notes |
|---|---|---|
| NeoForge | Required | Loader, tested on `21.1.221`. |
| Refined Storage | Required or Optional | Required if this is a standalone RS-only page. Optional if it stays under the main ArcadiaTweaks page. |

## Project summary

```text
Server-side Refined Storage optimization patch for RS2 networks. Reduces repeated node/transmitter activeness checks while keeping importer/exporter work cadence intact.
```

## Release title

```text
ArcadiaTweaks 0.1.1 - Refined Storage patch only
```

## Changelog

```markdown
## 0.1.1 - Refined Storage patch only

Single-patch ArcadiaTweaks build containing only the Refined Storage optimization module.

### Optimizations

- Coalesces repeated network node activeness checks.
- Coalesces repeated network transmitter activeness checks.
- Keeps importer/exporter work cadence intact.
- Adds structural mixin safety: if RS internals change, only the affected mixin is skipped.

### Admin GUI

Use:

```text
/refinedstoragepatch gui
```

### Production notes

Recommended balanced interval: `20` ticks.

Install only one ArcadiaTweaks variant at a time. This jar excludes BotanyPots and Mekanism patch classes and mixin configs.
```

## Project description

```markdown
# ArcadiaTweaks - Refined Storage Patch

This is the Refined Storage-only ArcadiaTweaks build for Refined Storage 2 on NeoForge 1.21.1.

It targets repeated RS network node and transmitter activeness checks visible in Spark on large production servers.

## What it optimizes

The patch coalesces repeated activeness checks for RS network block entities. It is intentionally conservative: importer/exporter work cadence is not globally slowed. The goal is to reduce repeated state checks without delaying actual network work more than necessary.

Recommended balanced settings:

```toml
[refinedstorage]
activeness_check_coalescing_enabled = true
activeness_check_interval_ticks = 20
```

Use `10` if you want quicker redstone/energy active-state response.
Use `1` to effectively disable the coalescing interval without disabling the whole module.

## Command

```text
/refinedstoragepatch gui
```

## Safety model

The patch is guarded by target structure, not by Refined Storage version string. If a future RS update changes the target class or method signature, the affected Mixin is skipped and ArcadiaTweaks logs a short warning.

## Installation

Drop the jar into the server `mods/` folder.

Server-side only. Clients do not need it.

Do not install multiple ArcadiaTweaks variants together.

## License

MIT
```
