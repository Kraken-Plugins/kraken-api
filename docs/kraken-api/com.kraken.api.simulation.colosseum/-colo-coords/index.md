//[kraken-api](../../../index.md)/[com.kraken.api.simulation.colosseum](../index.md)/[ColoCoords](index.md)

# ColoCoords

[Kraken API]\
class [ColoCoords](index.md)

Bit-packing helpers for arena-local tile coordinates. 

The colosseum simulation works in arena-local coordinates where `(0, 0)` is the south-west corner of the captured grid and both axes fit in 6 bits (max grid 64x64). A position is packed as `x | (y << 6)` into the low 12 bits of a short, which keeps simulation state copies down to primitive array copies with no object churn.

## Properties

| Name | Summary |
|---|---|
| [MAX_DIMENSION](-m-a-x_-d-i-m-e-n-s-i-o-n.md) | [Kraken API]<br>val [MAX_DIMENSION](-m-a-x_-d-i-m-e-n-s-i-o-n.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) = 64<br>Maximum grid dimension supported by the 6-bit packing. |
| [NONE](-n-o-n-e.md) | [Kraken API]<br>val [NONE](-n-o-n-e.md): [Short](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-short/index.html) = -1<br>Sentinel packed position meaning &quot;none&quot;. |

## Functions

| Name | Summary |
|---|---|
| [chebyshev](chebyshev.md) | [Kraken API]<br>open fun [chebyshev](chebyshev.md)(a: [Short](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-short/index.html), b: [Short](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-short/index.html)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)<br>Chebyshev distance between two packed positions. |
| [isPresent](is-present.md) | [Kraken API]<br>open fun [isPresent](is-present.md)(packed: [Short](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-short/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [offset](offset.md) | [Kraken API]<br>open fun [offset](offset.md)(packed: [Short](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-short/index.html), dx: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), dy: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Short](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-short/index.html)<br>Offsets a packed position. |
| [pack](pack.md) | [Kraken API]<br>open fun [pack](pack.md)(x: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), y: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Short](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-short/index.html)<br>Packs local coordinates into a short. |
| [x](x.md) | [Kraken API]<br>open fun [x](x.md)(packed: [Short](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-short/index.html)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [y](y.md) | [Kraken API]<br>open fun [y](y.md)(packed: [Short](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-short/index.html)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
