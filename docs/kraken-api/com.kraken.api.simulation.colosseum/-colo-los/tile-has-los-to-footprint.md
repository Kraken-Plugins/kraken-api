//[kraken-api](../../../index.md)/[com.kraken.api.simulation.colosseum](../index.md)/[ColoLos](index.md)/[tileHasLosToFootprint](tile-has-los-to-footprint.md)

# tileHasLosToFootprint

[Kraken API]\
open fun [tileHasLosToFootprint](tile-has-los-to-footprint.md)(grid: [ColoGrid](../-colo-grid/index.md), from: [Short](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-short/index.html), range: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), targetAnchor: [Short](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-short/index.html), targetSize: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Checks whether a single-tile attacker (the player) can hit an NPC footprint.

#### Return

true when the attacker can hit the target footprint.

#### Parameters

Kraken API

| | |
|---|---|
| grid | collision grid. |
| from | packed attacker tile. |
| range | attack range in tiles (1 for melee). |
| targetAnchor | packed south-west anchor of the target footprint. |
| targetSize | target footprint size. |
