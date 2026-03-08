//[lib](../../../index.md)/[com.kraken.api.service.pathfinding](../index.md)/[SparseBitSet](index.md)/[previousSetBit](previous-set-bit.md)

# previousSetBit

[Kraken API]\
open fun [previousSetBit](previous-set-bit.md)(i: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)

Returns the index of the nearest bit that is set to `true` that occurs on or before the specified starting index. If no such bit exists, or if `-1` is given as the starting index, then `-1` is returned.

#### Return

the index of the previous set bit, or `-1` if there is no such bit

#### Since

1.2

#### Parameters

Kraken API

| | |
|---|---|
| i | the index to start checking from (inclusive) |

#### See also

| |
|---|
| [BitSet](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/BitSet.html#previousSetBit-int-) |

#### Throws

| | |
|---|---|
| [IndexOutOfBoundsException](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/IndexOutOfBoundsException.html) | if the specified index is less than `-1` |
