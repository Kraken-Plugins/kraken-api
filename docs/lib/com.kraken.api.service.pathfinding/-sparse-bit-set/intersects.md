//[lib](../../../index.md)/[com.kraken.api.service.pathfinding](../index.md)/[SparseBitSet](index.md)/[intersects](intersects.md)

# intersects

[Kraken API]\
open fun [intersects](intersects.md)(i: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), j: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), b: [SparseBitSet](index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Returns true if the specified `SparseBitSet` has any bits within the given range `i` (inclusive) to `j` (exclusive) set to `true` that are also set to `true` in the same range of this `SparseBitSet`.

#### Return

the boolean indicating whether this SparseBitSet intersects the specified SparseBitSet

#### Since

1.6

#### Parameters

Kraken API

| | |
|---|---|
| i | index of the first bit to include |
| j | index after the last bit to include |
| b | the SparseBitSet with which to intersect |

#### Throws

| | |
|---|---|
| [IndexOutOfBoundsException](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/IndexOutOfBoundsException.html) | if `i` is negative or equal to Integer.MAX_VALUE, or `j` is negative, or `i` is larger than `j` |

[Kraken API]\
open fun [intersects](intersects.md)(b: [SparseBitSet](index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Returns true if the specified `SparseBitSet` has any bits set to `true` that are also set to `true` in this `SparseBitSet`.

#### Return

boolean indicating whether this SparseBitSet intersects the specified SparseBitSet

#### Since

1.6

#### Parameters

Kraken API

| | |
|---|---|
| b | a SparseBitSet with which to intersect |
