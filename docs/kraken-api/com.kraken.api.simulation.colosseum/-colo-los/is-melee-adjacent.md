//[kraken-api](../../../index.md)/[com.kraken.api.simulation.colosseum](../index.md)/[ColoLos](index.md)/[isMeleeAdjacent](is-melee-adjacent.md)

# isMeleeAdjacent

[Kraken API]\
open fun [isMeleeAdjacent](is-melee-adjacent.md)(anchorX: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), anchorY: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), size: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), targetX: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), targetY: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Bounding-box melee adjacency: the target must be orthogonally adjacent to the footprint edge, never diagonal.

#### Return

true when melee-reachable.

#### Parameters

Kraken API

| | |
|---|---|
| anchorX | footprint south-west x. |
| anchorY | footprint south-west y. |
| size | footprint size. |
| targetX | target x. |
| targetY | target y. |
