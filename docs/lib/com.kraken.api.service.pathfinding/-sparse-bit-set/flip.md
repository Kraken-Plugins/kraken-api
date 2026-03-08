//[lib](../../../index.md)/[com.kraken.api.service.pathfinding](../index.md)/[SparseBitSet](index.md)/[flip](flip.md)

# flip

[Kraken API]\
open fun [flip](flip.md)(i: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html))

Sets the bit at the specified index to the complement of its current value.

#### Since

1.6

#### Parameters

Kraken API

| | |
|---|---|
| i | the index of the bit to flip |

#### Throws

| | |
|---|---|
| [IndexOutOfBoundsException](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/IndexOutOfBoundsException.html) | if the specified index is negative or equal to Integer.MAX_VALUE |

[Kraken API]\
open fun [flip](flip.md)(i: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), j: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html))

Sets each bit from the specified `i` (inclusive) to the specified `j` (exclusive) to the complement of its current value.

#### Since

1.6

#### Parameters

Kraken API

| | |
|---|---|
| i | index of the first bit to flip |
| j | index after the last bit to flip |

#### Throws

| | |
|---|---|
| [IndexOutOfBoundsException](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/IndexOutOfBoundsException.html) | if `i` is negative or is equal to Integer.MAX_VALUE, or `j` is negative, or `i` is larger than `j` |
