//[lib](../../../index.md)/[com.kraken.api.service.pathfinding](../index.md)/[SparseBitSet](index.md)/[get](get.md)

# get

[Kraken API]\
open fun [get](get.md)(i: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Returns the value of the bit with the specified index. The value is `true` if the bit with the index `i` is currently set in this `SparseBitSet`; otherwise, the result is `false`.

#### Return

the boolean value of the bit with the specified index.

#### Since

1.6

#### Parameters

Kraken API

| | |
|---|---|
| i | the bit index |

#### Throws

| | |
|---|---|
| [IndexOutOfBoundsException](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/IndexOutOfBoundsException.html) | if the specified index is negative or equal to Integer.MAX_VALUE |

[Kraken API]\
open fun [get](get.md)(i: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), j: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [SparseBitSet](index.md)

Returns a new `SparseBitSet` composed of bits from this `SparseBitSet` from `i` (inclusive) to `j` (exclusive).

#### Return

a new SparseBitSet from a range of this SparseBitSet

#### Since

1.6

#### Parameters

Kraken API

| | |
|---|---|
| i | index of the first bit to include |
| j | index after the last bit to include |

#### Throws

| | |
|---|---|
| [IndexOutOfBoundsException](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/IndexOutOfBoundsException.html) | if `i` is negative or is equal to Integer.MAX_VALUE, or `j` is negative, or `i` is larger than `j` |
