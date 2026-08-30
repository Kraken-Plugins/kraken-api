# Migrating to Kraken API 5.0

5.0 is a breaking release that makes the query layer's null handling and spatial vocabulary
consistent everywhere. There are no deprecation shims; every rewrite below is mechanical.

## Single-valued terminals return `Optional`

`first()`, `firstMatching(...)`, `random()`, `nearest()`, and `nearestTo(...)` on every query now
return `java.util.Optional` instead of a nullable entity. `EquipmentQuery.inSlot(...)` and
`DepositBoxQuery.inSlot(...)` do the same.

| Before | After |
|---|---|
| `q.first().interact("Open")` | `q.interact("Open")` |
| `q.nearest().interact("Chop")` | `q.sortByDistance().interact("Chop")` |
| `Entity e = q.first(); if (e != null) ...` | `q.first().ifPresent(e -> ...)` or `q.first().orElse(null)` |
| `q.first() != null` | `q.isPresent()` |
| `q.nearest().take()` | `q.nearest().map(GroundObjectEntity::take).orElse(false)` |

## Entity-level `isNull()` / `isPresent()` are gone

An entity in hand is always real now — "maybe" lives in the `Optional` the terminal returned.
`q.first().isPresent()` still compiles and now actually works (it used to NPE on the empty case).
Replace `entity.isNull()` on an entity you hold with a check on how you obtained it.

## One spatial vocabulary

`NpcQuery`, `PlayerQuery`, `GameObjectQuery`, `TileObjectQuery`, `GroundObjectQuery` (and the new
`ProjectileQuery` / `GraphicsObjectQuery`) share `AbstractSpatialQuery`: `within`, `withinArea`,
`at`, `reachable`, `sortByDistance`, `sortByDistanceTo`, `nearest`, `nearestTo` — identical names,
anchors, and plane semantics on all of them. Renames and removals:

| Before | After |
|---|---|
| `players().withinDistance(n)` | `players().within(n)` |
| `tileObjects().near(point, n)` | `tileObjects().within(point, n)` |
| `gameObjects().within(localPoint, n)` | `gameObjects().within(worldPoint, n)` |
| `npcs().nearestTo(point).first()` | `npcs().nearestTo(point)` (now a terminal returning `Optional`) |

`PlayerQuery` gains `reachable()`; `GroundObjectQuery` gains `withinArea(...)`. Distances are
same-plane only; player-anchored filters return empty results when there is no local player.

## One container vocabulary

`InventoryQuery`, `BankQuery`, `BankInventoryQuery`, `DepositBoxQuery`, `ShopInventoryQuery` share
`AbstractContainerQuery`: `inSlot`, `noted`, `unnoted`, `stackable`, `quantityGreaterThan`,
`withAction`, `hasItem`, `hasItems`. `BankQuery` gains all of them.

| Before | After |
|---|---|
| `inventory().inSlot(3)` (returned entity) | `inventory().inSlot(3).first()` (filter + `Optional` terminal) |
| `bankEntity.count()` / `depositBoxEntity.count()` etc. | `entity.getQuantity()` |

## New queries

`ctx.projectiles()` and `ctx.graphicsObjects()` query projectiles in flight and active graphics
objects. Both are spatial queries with the full shared vocabulary; their entities are observations
only — `interact(...)` always returns `false`.
