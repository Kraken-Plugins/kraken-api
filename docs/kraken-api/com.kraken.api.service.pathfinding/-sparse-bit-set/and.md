//[kraken-api](../../../index.md)/[com.kraken.api.service.pathfinding](../index.md)/[SparseBitSet](index.md)/[and](and.md)

# and

[Kraken API]\
open fun [and](and.md)(i: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), value: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html))

Performs a logical **AND** of the addressed target bit with the argument value. This bit set is modified so that the addressed bit has the value `true` if and only if it both initially had the value `true` and the argument value is also `true`.

#### Since

1.6

#### Parameters

Kraken API

| | |
|---|---|
| i | a bit index |
| value | a boolean value to **AND** with that bit |

#### Throws

| | |
|---|---|
| [IndexOutOfBoundsException](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/IndexOutOfBoundsException.html) | if the specified index is negative or equal to Integer.MAX_VALUE |

[Kraken API]\
open fun [and](and.md)(i: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), j: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), b: [SparseBitSet](index.md))

Performs a logical **AND** of this target bit set with the argument bit set within the given range of bits. Within the range, this bit set is modified so that each bit in it has the value `true` if and only if it both initially had the value `true` and the corresponding bit in the bit set argument also had the value `true`. Outside the range, this set is not changed.

#### Since

1.6

#### Parameters

Kraken API

| | |
|---|---|
| i | index of the first bit to be included in the operation |
| j | index after the last bit to included in the operation |
| b | a SparseBitSet |

#### Throws

| | |
|---|---|
| [IndexOutOfBoundsException](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/IndexOutOfBoundsException.html) | if `i` is negative or equal to Integer.MAX_VALUE, or `j` is negative, or `i` is larger than `j` |

[Kraken API]\
open fun [and](and.md)(b: [SparseBitSet](index.md))

Performs a logical **AND** of this target bit set with the argument bit set. This bit set is modified so that each bit in it has the value `true` if and only if it both initially had the value `true` and the corresponding bit in the bit set argument also had the value `true`.

#### Since

1.6

#### Parameters

Kraken API

| | |
|---|---|
| b | a SparseBitSet |

[Kraken API]\
open fun [and](and.md)(a: [SparseBitSet](index.md), b: [SparseBitSet](index.md)): [SparseBitSet](index.md)

Performs a logical **AND** of the two given `SparseBitSet`s. The returned `SparseBitSet` is created so that each bit in it has the value `true` if and only if both the given sets initially had the corresponding bits `true`, otherwise `false`.

#### Return

a new SparseBitSet representing the **AND** of the two sets

#### Since

1.6

#### Parameters

Kraken API

| | |
|---|---|
| a | a SparseBitSet |
| b | another SparseBitSet |
