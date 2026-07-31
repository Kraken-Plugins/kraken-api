//[kraken-api](../../../index.md)/[com.kraken.api.simulation.colosseum](../index.md)/[ColoLos](index.md)/[footprintHasLosTo](footprint-has-los-to.md)

# footprintHasLosTo

[Kraken API]\
open fun [footprintHasLosTo](footprint-has-los-to.md)(grid: [ColoGrid](../-colo-grid/index.md), anchor: [Short](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-short/index.html), size: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), range: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), target: [Short](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-short/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Checks whether an attacker footprint has line of sight (and range) to a single-tile target. 

Order of checks matches the validated colosseum simulator: a target underneath the attacker footprint is never visible; melee uses bounding-box adjacency; otherwise the footprint tile nearest the target is clamped and a tile raycast decides.

#### Return

true when the attacker can hit the target from its current position.

#### Parameters

Kraken API

| | |
|---|---|
| grid | collision grid. |
| anchor | packed south-west anchor of the attacker footprint. |
| size | attacker footprint size. |
| range | attack range in tiles (1 for melee). |
| target | packed target tile. |
