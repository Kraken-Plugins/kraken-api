//[lib](../../../index.md)/[com.kraken.api.service.pathfinding](../index.md)/[SparseBitSet](index.md)/[nextClearBit](next-clear-bit.md)

# nextClearBit

[Kraken API]\
open fun [nextClearBit](next-clear-bit.md)(i: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)

Returns the index of the first bit that is set to `false` that occurs on or after the specified starting index.

#### Return

the index of the next clear bit, or -1 if there is no such bit

#### Since

1.6

#### Parameters

Kraken API

| | |
|---|---|
| i | the index to start checking from (inclusive) |

#### Throws

| | |
|---|---|
| [IndexOutOfBoundsException](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/IndexOutOfBoundsException.html) | if the specified index is negative |
