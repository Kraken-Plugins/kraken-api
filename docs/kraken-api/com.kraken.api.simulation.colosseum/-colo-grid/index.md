//[kraken-api](../../../index.md)/[com.kraken.api.simulation.colosseum](../index.md)/[ColoGrid](index.md)

# ColoGrid

[Kraken API]\
class [ColoGrid](index.md)

Immutable arena collision grid backed by one 64-bit row mask per tile row. 

Two masks are kept: movement blocking and line-of-sight blocking. Inside the colosseum arena every obstacle (boundary and pillars) blocks both, but the masks are captured independently from the client collision map so the engine stays correct if that ever differs. Movement legality for multi-tile NPCs is answered from precomputed &quot;eroded&quot; masks: `blockedForSize[s]` has a bit set where a size-`s` footprint anchored (south-west) on that tile would overlap a blocked tile or leave the grid.

All queries are static-cost bit tests, safe to call millions of times per decision.

## Properties

| Name | Summary |
|---|---|
| [MAX_NPC_SIZE](-m-a-x_-n-p-c_-s-i-z-e.md) | [Kraken API]<br>val [MAX_NPC_SIZE](-m-a-x_-n-p-c_-s-i-z-e.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) = 5<br>Largest NPC footprint the grid precomputes erosion for (Sol Heredit is excluded). |

## Functions

| Name | Summary |
|---|---|
| [baseX](base-x.md) | [Kraken API]<br>open fun [baseX](base-x.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [baseY](base-y.md) | [Kraken API]<br>open fun [baseY](base-y.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [fromCollisionFlags](from-collision-flags.md) | [Kraken API]<br>open fun [fromCollisionFlags](from-collision-flags.md)(flags: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)&gt;&gt;, sceneOriginX: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), sceneOriginY: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), width: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), height: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), baseX: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), baseY: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), plane: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [ColoGrid](index.md)<br>Builds a grid from a RuneLite scene collision map. |
| [height](height.md) | [Kraken API]<br>open fun [height](height.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [inBounds](in-bounds.md) | [Kraken API]<br>open fun [inBounds](in-bounds.md)(x: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), y: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [isFootprintFree](is-footprint-free.md) | [Kraken API]<br>open fun [isFootprintFree](is-footprint-free.md)(x: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), y: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), size: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Checks whether a size-`size` footprint anchored south-west on `(x, y)` fits without touching blocked tiles or leaving the grid. |
| [isLosBlocked](is-los-blocked.md) | [Kraken API]<br>open fun [isLosBlocked](is-los-blocked.md)(x: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), y: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [isMoveBlocked](is-move-blocked.md) | [Kraken API]<br>open fun [isMoveBlocked](is-move-blocked.md)(x: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), y: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [plane](plane.md) | [Kraken API]<br>open fun [plane](plane.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [synthetic](synthetic.md) | [Kraken API]<br>open fun [synthetic](synthetic.md)(width: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), height: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), baseX: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), baseY: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), plane: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), blockedTiles: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[Short](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-short/index.html)&gt;): [ColoGrid](index.md)<br>Builds a synthetic grid from explicit blocked tiles, used by tests and offline tooling. |
| [toLocal](to-local.md) | [Kraken API]<br>open fun [toLocal](to-local.md)(worldX: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), worldY: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Short](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-short/index.html)<br>Converts world coordinates to a packed local position. |
| [toWorld](to-world.md) | [Kraken API]<br>open fun [toWorld](to-world.md)(packed: [Short](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-short/index.html)): WorldPoint<br>Converts a local packed position to a world point. |
| [width](width.md) | [Kraken API]<br>open fun [width](width.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
