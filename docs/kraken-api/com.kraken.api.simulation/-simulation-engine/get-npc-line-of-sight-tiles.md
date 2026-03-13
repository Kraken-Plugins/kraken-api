//[kraken-api](../../../index.md)/[com.kraken.api.simulation](../index.md)/[SimulationEngine](index.md)/[getNpcLineOfSightTiles](get-npc-line-of-sight-tiles.md)

# getNpcLineOfSightTiles

[Kraken API]\
open fun [getNpcLineOfSightTiles](get-npc-line-of-sight-tiles.md)(state: [SimulationState](../-simulation-state/index.md), npcSlot: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;

Returns npc-visible tiles using configured range.

#### Return

tiles.

#### Parameters

Kraken API

| | |
|---|---|
| state | state. |
| npcSlot | npc slot. |

[Kraken API]\
open fun [getNpcLineOfSightTiles](get-npc-line-of-sight-tiles.md)(state: [SimulationState](../-simulation-state/index.md), npcSlot: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), range: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;

Returns npc-visible tiles using explicit range.

#### Return

tiles.

#### Parameters

Kraken API

| | |
|---|---|
| state | state. |
| npcSlot | npc slot. |
| range | range. |
