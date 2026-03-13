//[kraken-api](../../../index.md)/[com.kraken.api.service.pathfinding](../index.md)/[SparseBitSet](index.md)/[or](or.md)

# or

[Kraken API]\
open fun [or](or.md)(i: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), value: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html))

Performs a logical **OR** of the addressed target bit with the argument value. This bit set is modified so that the addressed bit has the value `true` if and only if it both initially had the value `true` or the argument value is `true`.

#### Since

1.6

#### Parameters

Kraken API

| | |
|---|---|
| i | a bit index |
| value | a boolean value to OR with that bit |

#### Throws

| | |
|---|---|
| [IndexOutOfBoundsException](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/IndexOutOfBoundsException.html) | if the specified index is negative or equal to Integer.MAX_VALUE |

[Kraken API]\
open fun [or](or.md)(i: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), j: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), b: [SparseBitSet](index.md))

Performs a logical **OR** of the addressed target bit with the argument value within the given range. This bit set is modified so that within the range a bit in it has the value `true` if and only if it either already had the value `true` or the corresponding bit in the bit set argument has the value `true`. Outside the range this set is not changed.

#### Since

1.6

#### Parameters

Kraken API

| | |
|---|---|
| i | index of the first bit to be included in the operation |
| j | index after the last bit to included in the operation |
| b | the SparseBitSet with which to perform the **OR**operation with this SparseBitSet |

#### Throws

| | |
|---|---|
| [IndexOutOfBoundsException](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/IndexOutOfBoundsException.html) | if `i` is negative or equal to Integer.MAX_VALUE, or `j` is negative, or `i` is larger than `j` |

[Kraken API]\
open fun [or](or.md)(b: [SparseBitSet](index.md))

Performs a logical **OR** of this bit set with the bit set argument. This bit set is modified so that a bit in it has the value `true` if and only if it either already had the value `true` or the corresponding bit in the bit set argument has the value `true`.

#### Since

1.6

#### Parameters

Kraken API

| | |
|---|---|
| b | the SparseBitSet with which to perform the **OR**operation with this SparseBitSet |

[Kraken API]\
open fun [or](or.md)(a: [SparseBitSet](index.md), b: [SparseBitSet](index.md)): [SparseBitSet](index.md)

Performs a logical **OR** of the two given `SparseBitSet`s. The returned `SparseBitSet` is created so that a bit in it has the value `true` if and only if it either had the value `true` in the set given by the first arguemetn or had the value `true` in the second argument, otherwise `false`.

#### Return

new SparseBitSet representing the **OR** of the two sets

#### Since

1.6

#### Parameters

Kraken API

| | |
|---|---|
| a | a SparseBitSet |
| b | another SparseBitSet |
