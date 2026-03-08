//[lib](../../../index.md)/[com.kraken.api.service.pathfinding](../index.md)/[SparseBitSet](index.md)/[set](set.md)

# set

[Kraken API]\
open fun [set](set.md)(i: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html))

Sets the bit at the specified index.

#### Since

1.6

#### Parameters

Kraken API

| | |
|---|---|
| i | a bit index |

#### Throws

| | |
|---|---|
| [IndexOutOfBoundsException](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/IndexOutOfBoundsException.html) | if the specified index is negative or equal to Integer.MAX_VALUE |

[Kraken API]\
open fun [set](set.md)(i: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), value: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html))

Sets the bit at the specified index to the specified value.

#### Since

1.6

#### Parameters

Kraken API

| | |
|---|---|
| i | a bit index |
| value | a boolean value to set |

#### Throws

| | |
|---|---|
| [IndexOutOfBoundsException](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/IndexOutOfBoundsException.html) | if the specified index is negative or equal to Integer.MAX_VALUE |

[Kraken API]\
open fun [set](set.md)(i: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), j: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html))

Sets the bits from the specified `i` (inclusive) to the specified `j` (exclusive) to `true`.

#### Since

1.6

#### Parameters

Kraken API

| | |
|---|---|
| i | index of the first bit to be set |
| j | index after the last bit to be se |

#### Throws

| | |
|---|---|
| [IndexOutOfBoundsException](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/IndexOutOfBoundsException.html) | if `i` is negative or is equal to Integer.MAX_INT, or `j` is negative, or `i` is larger than `j`. |

[Kraken API]\
open fun [set](set.md)(i: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), j: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), value: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html))

Sets the bits from the specified `i` (inclusive) to the specified `j` (exclusive) to the specified value.

#### Since

1.6

#### Parameters

Kraken API

| | |
|---|---|
| i | index of the first bit to be set |
| j | index after the last bit to be set |
| value | to which to set the selected bits |

#### Throws

| | |
|---|---|
| [IndexOutOfBoundsException](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/IndexOutOfBoundsException.html) | if `i` is negative or is equal to Integer.MAX_VALUE, or `j` is negative, or `i` is larger than `j` |
