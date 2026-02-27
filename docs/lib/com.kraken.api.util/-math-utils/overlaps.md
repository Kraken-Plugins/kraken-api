//[lib](../../../index.md)/[com.kraken.api.util](../index.md)/[MathUtils](index.md)/[overlaps](overlaps.md)

# overlaps

[Kraken API]\
open fun [overlaps](overlaps.md)(aX: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), aY: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), aSize: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), bX: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), bY: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), bSize: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Returns whether two square footprints overlap.

#### Return

True if any of the tiles overlap and false otherwise.

#### Parameters

Kraken API

| | |
|---|---|
| aX | The x coordinate of the SW first tile |
| aY | The y coordinate of the SW first tile |
| aSize | The size of the set of tiles |
| bX | The x coordinate of the SW second tile |
| bY | The y coordinate of the SW second tile |
| bSize | The size of the second set of tiles |
