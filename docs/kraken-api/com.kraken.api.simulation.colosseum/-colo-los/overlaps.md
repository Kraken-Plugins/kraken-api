//[kraken-api](../../../index.md)/[com.kraken.api.simulation.colosseum](../index.md)/[ColoLos](index.md)/[overlaps](overlaps.md)

# overlaps

[Kraken API]\
open fun [overlaps](overlaps.md)(anchorX: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), anchorY: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), size: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), tileX: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), tileY: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Square footprint overlap test (south-west anchored, second footprint is 1x1).

#### Return

true when the tile lies inside the footprint.

#### Parameters

Kraken API

| | |
|---|---|
| anchorX | first footprint south-west x. |
| anchorY | first footprint south-west y. |
| size | first footprint size. |
| tileX | single tile x. |
| tileY | single tile y. |
