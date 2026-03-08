//[lib](../../../index.md)/[com.kraken.api.service.pathfinding](../index.md)/[SparseBitSet](index.md)/[clear](clear.md)

# clear

[Kraken API]\
open fun [clear](clear.md)(i: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html))

Sets the bit at the specified index to `false`.

#### Since

1.6

#### Parameters

Kraken API

| | |
|---|---|
| i | a bit index. |

#### Throws

| | |
|---|---|
| [IndexOutOfBoundsException](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/IndexOutOfBoundsException.html) | if the specified index is negative or equal to Integer.MAX_VALUE. |

[Kraken API]\
open fun [clear](clear.md)(i: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), j: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html))

Sets the bits from the specified `i` (inclusive) to the specified `j` (exclusive) to `false`.

#### Since

1.6

#### Parameters

Kraken API

| | |
|---|---|
| i | index of the first bit to be cleared |
| j | index after the last bit to be cleared |

#### Throws

| | |
|---|---|
| [IndexOutOfBoundsException](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/IndexOutOfBoundsException.html) | if `i` is negative or equal to Integer.MAX_VALUE, or `j` is negative, or `i` is larger than `j` |

[Kraken API]\
open fun [clear](clear.md)()

Sets all of the bits in this `SparseBitSet` to `false`.

#### Since

1.6
