//[kraken-api](../../../index.md)/[com.kraken.api.simulation.colosseum](../index.md)/[ColoScratch](index.md)/[approachField](approach-field.md)

# approachField

[Kraken API]\
open fun [approachField](approach-field.md)(grid: [ColoGrid](../-colo-grid/index.md), playerPos: [Short](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-short/index.html), size: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[Byte](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte/index.html)&gt;

Returns the BFS approach field for a route-finding NPC of the given size hunting the player: distance from any anchor tile to the nearest legal melee-attack anchor.

#### Return

distance field indexed by anchor `y << 6 | x`.

#### Parameters

Kraken API

| | |
|---|---|
| grid | collision grid. |
| playerPos | packed player position. |
| size | npc footprint size. |
