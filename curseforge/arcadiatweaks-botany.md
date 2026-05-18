# ArcadiaTweaks Botany Patch - CurseForge publication

## Recommendation

Publish this as a separate file under the main **ArcadiaTweaks** CurseForge project, not as a separate project page, unless you have a strong discoverability reason.

Do not install this jar together with the all-in-one jar or other single-patch variants. All variants use the same mod id: `arcadiatweaks`.

## File metadata

| Field | Value |
|---|---|
| File | `arcadiatweaks-0.1.1-botany.jar` |
| Version | `0.1.1-botany` |
| Release type | Beta |
| Minecraft | `1.21.1` |
| Loader | NeoForge |
| Side | Server-side only |
| License | MIT |

## CurseForge relations

| Mod | Relation | Notes |
|---|---|---|
| NeoForge | Required | Loader, tested on `21.1.221`. |
| BotanyPots | Required or Optional | Required if this is a standalone Botany-only page. Optional if it stays under the main ArcadiaTweaks page. |
| Bookshelf | Optional | BotanyPots dependency. |
| Prickle | Optional | BotanyPots dependency. |

## Project summary

```text
Server-side BotanyPots optimization patch for large farms. Mixin-based, configurable, server-only, with tick coalescing, cache layers and hopper backoff.
```

## Release title

```text
ArcadiaTweaks 0.1.1 - BotanyPots patch only
```

## Changelog

```markdown
## 0.1.1 - BotanyPots patch only

Single-patch ArcadiaTweaks build containing only the BotanyPots optimization module.

### Optimizations

- Crop/soil compatibility memoization.
- Pot tick coalescing with growth progress compensation.
- Hopper export backoff after repeated failed exports.
- Required growth tick cache.
- Optional light update flag downgrade, disabled by default.

### Admin GUI

Use:

```text
/botanypatch gui
```

### Production notes

Install only one ArcadiaTweaks variant at a time. This jar excludes Refined Storage and Mekanism patch classes and mixin configs.
```

## Project description

```markdown
# ArcadiaTweaks - BotanyPots Patch

This is the BotanyPots-only ArcadiaTweaks build.

It targets the server-side hot paths of large BotanyPots farms without changing recipes, registries or client rendering.

## What it optimizes

| Strategy | Description | Default |
|---|---|---|
| Crop/soil cache | Caches repeated crop x soil compatibility checks between inventory/state changes. | On |
| Tick coalescing | Runs expensive pot logic once every N ticks while compensating growth progress. | On, N=4 |
| Hopper backoff | Backs off repeated failed exports into full or missing inventories. | On |
| Growth tick cache | Caches required growth tick calculations per pot. | On |
| Light flag downgrade | Avoids some light recalculation flags when only inventory changed. | Off |

Recommended balanced settings:

```toml
[botany]
matches_cache_enabled = true
tick_coalescing_enabled = true
tick_coalescing_n = 4
hopper_backoff_enabled = true
hopper_backoff_min_ticks = 16
hopper_backoff_max_ticks = 64
required_growth_ticks_cache_enabled = true
light_flag_downgrade_enabled = false
safety_revalidate_period_ticks = 200
```

## Command

```text
/botanypatch gui
```

## Installation

Drop the jar into the server `mods/` folder.

Server-side only. Clients do not need it.

Do not install multiple ArcadiaTweaks variants together.

## License

MIT
```
