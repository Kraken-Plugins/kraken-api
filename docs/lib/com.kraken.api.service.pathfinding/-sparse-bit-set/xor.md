//[lib](../../../index.md)/[com.kraken.api.service.pathfinding](../index.md)/[SparseBitSet](index.md)/[xor](xor.md)

# xor

[Kraken API]\
open fun [xor](xor.md)(i: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), value: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html))

Performs a logical **XOR** of the addressed target bit with the argument value. This bit set is modified so that the addressed bit has the value `true` if and only one of the following statements holds: 

- The addressed bit initially had the value `true`, and the value of the argument is `false`.
- The bit initially had the value `false`, and the value of the argument is `true`.

#### Since

1.6

#### Parameters

Kraken API

| | |
|---|---|
| i | a bit index |
| value | a boolean value to **XOR** with that bit |

#### Throws

| | |
|---|---|
| [IndexOutOfBoundsException](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/IndexOutOfBoundsException.html) | if the specified index is negative or equal to Integer.MAX_VALUE |

[Kraken API]\
open fun [xor](xor.md)(i: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), j: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), b: [SparseBitSet](index.md))

Performs a logical **XOR** of this bit set with the bit set argument within the given range. This resulting bit set is computed so that a bit within the range in it has the value `true` if and only if one of the following statements holds: 

- The bit initially had the value `true`, and the corresponding bit in the argument set has the value `false`.
- The bit initially had the value `false`, and the corresponding bit in the argument set has the value `true`.

 Outside the range this set is not changed.

#### Since

1.6

#### Parameters

Kraken API

| | |
|---|---|
| i | index of the first bit to be included in the operation |
| j | index after the last bit to included in the operation |
| b | the SparseBitSet with which to perform the **XOR**operation with this SparseBitSet |

#### Throws

| | |
|---|---|
| [IndexOutOfBoundsException](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/IndexOutOfBoundsException.html) | if `i` is negative or equal to Integer.MAX_VALUE, or `j` is negative, or `i` is larger than `j` |

[Kraken API]\
open fun [xor](xor.md)(b: [SparseBitSet](index.md))

Performs a logical **XOR** of this bit set with the bit set argument. This resulting bit set is computed so that a bit in it has the value `true` if and only if one of the following statements holds: 

- The bit initially had the value `true`, and the corresponding bit in the argument set has the value `false`.
- The bit initially had the value `false`, and the corresponding bit in the argument set has the value `true`.

#### Since

1.6

#### Parameters

Kraken API

| | |
|---|---|
| b | the SparseBitSet with which to perform the **XOR**operation with thisSparseBitSet |

[Kraken API]\
open fun [xor](xor.md)(a: [SparseBitSet](index.md), b: [SparseBitSet](index.md)): [SparseBitSet](index.md)

Performs a logical **XOR** of the two given `SparseBitSet`s. The resulting bit set is created so that a bit in it has the value `true` if and only if one of the following statements holds: 

- A bit in the first argument has the value `true`, and the corresponding bit in the second argument has the value `false`.
- A bit in the first argument has the value `false`, and the corresponding bit in the second argument has the value `true`.

#### Return

a new SparseBitSet representing the **XOR** of the two sets

#### Since

1.6

#### Parameters

Kraken API

| | |
|---|---|
| a | a SparseBitSet |
| b | another SparseBitSet |
