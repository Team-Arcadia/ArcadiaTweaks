# ArcadiaTweaks Mekanism Patch - CurseForge publication

## Recommendation

Publish this as a separate file under the main **ArcadiaTweaks** CurseForge project. Avoid a separate page unless you clearly warn users not to install multiple ArcadiaTweaks variants together.

All variants use the same mod id: `arcadiatweaks`.

## File metadata

| Field | Value |
|---|---|
| File | `arcadiatweaks-0.1.1-mekanism.jar` |
| Version | `0.1.1-mekanism` |
| Release type | Beta |
| Minecraft | `1.21.1` |
| Loader | NeoForge |
| Side | Server + client |
| License | MIT |

## CurseForge relations

| Mod | Relation | Notes |
|---|---|---|
| NeoForge | Required | Loader, tested on `21.1.221`. |
| Mekanism | Required or Optional | Required if this is a standalone Mekanism-only page. Optional if it stays under the main ArcadiaTweaks page. |

## Project summary

```text
Mekanism optimization patch for large idle cable, pipe and tube networks, plus a client-side safe rendering path for colored Logistical Transporters. Adds conservative backoff to repeated no-op transmitter, network and logistical transporter idle path scans.
```

## Release title

```text
ArcadiaTweaks 0.1.1 - Mekanism patch only
```

## Changelog

```markdown
## 0.1.1 - Mekanism patch only

Single-patch ArcadiaTweaks build containing only the Mekanism optimization module.

### Optimizations

- Universal Cable pull backoff after repeated no-op pulls.
- Mechanical Pipe pull backoff after repeated no-op pulls.
- Pressurized Tube pull backoff after repeated no-op pulls.
- Energy/fluid/chemical network emit backoff after repeated no-op emits.
- Logistical Transporter idle path recalculation backoff for repeated no-target item routing.
- Client-side safe rendering for colored Logistical Transporters, keeping colors visible while guarding against progressive FPS/memory degradation.
- Heat networks are not touched.

### Admin GUI

Use:

```text
/mekanismpatch gui
```

### Production notes

Recommended balanced max backoff: `20` ticks.

Install only one ArcadiaTweaks variant at a time. This jar excludes BotanyPots and Refined Storage patch classes and mixin configs.
```

## Project description

```markdown
# ArcadiaTweaks - Mekanism Patch

This is the Mekanism-only ArcadiaTweaks build for NeoForge 1.21.1.

It targets repeated no-op transmitter, network and logistical transporter scans visible in Spark on large Mekanism cable, pipe and tube networks.

## What it optimizes

The patch adds conservative backoff when a transmitter or network repeatedly tries to pull or emit nothing. This avoids doing the same empty scan every tick on large idle networks.

Touched areas:

- Universal Cable
- Mechanical Pipe
- Pressurized Tube
- Energy network emit
- Fluid network emit
- Chemical network emit
- Logistical Transporter idle path recalculation

Heat networks are intentionally not patched.

Recommended balanced settings:

```toml
[mekanism]
transmitter_pull_backoff_enabled = true
transmitter_pull_backoff_max_ticks = 20
network_emit_backoff_enabled = true
network_emit_backoff_max_ticks = 20
logistical_transporter_idle_backoff_enabled = true
logistical_transporter_idle_backoff_max_ticks = 20
client_logistical_transporter_safe_rendering_enabled = true
client_transmitter_model_cache_max_entries = 4096
```

Aggressive profile for very large mostly-idle networks:

```toml
transmitter_pull_backoff_max_ticks = 40
network_emit_backoff_max_ticks = 40
logistical_transporter_idle_backoff_max_ticks = 40
```

Use the aggressive profile only if Spark still shows Mekanism network scans high after the balanced profile.

## Command

```text
/mekanismpatch gui
```

## Safety model

The patch is guarded by target structure, not by Mekanism version string. If a future Mekanism update changes the target class or method signature, the affected Mixin is skipped and ArcadiaTweaks logs a short warning.

## Installation

Drop the jar into the server `mods/` folder. For the M4 rendering safety patch, also ship it on clients through the launcher.

Server tick optimizations work server-side. The client rendering fix requires the client jar.

Do not install multiple ArcadiaTweaks variants together.

## License

MIT
```
