//[kraken-api](../../../index.md)/[com.kraken.api.simulation.colosseum](../index.md)/[ColoTick](index.md)/[npcHasLosToTile](npc-has-los-to-tile.md)

# npcHasLosToTile

[Kraken API]\
open fun [npcHasLosToTile](npc-has-los-to-tile.md)(s: [ColoState](../-colo-state/index.md), slot: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), tile: [Short](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-short/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Whether an NPC would have line of sight (and range) to a hypothetical player tile.

#### Return

true when the npc could hit that tile from where it stands.

#### Parameters

Kraken API

| | |
|---|---|
| s | state. |
| slot | npc slot. |
| tile | packed tile to test. |
