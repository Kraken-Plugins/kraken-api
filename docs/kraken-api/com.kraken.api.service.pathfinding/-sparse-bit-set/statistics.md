//[kraken-api](../../../index.md)/[com.kraken.api.service.pathfinding](../index.md)/[SparseBitSet](index.md)/[statistics](statistics.md)

# statistics

[Kraken API]\
open fun [statistics](statistics.md)(): [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)

Convenience method for statistics if the individual results are not needed.

#### Return

a String detailing the statistics of the bit set

#### Since

1.6

#### See also

| |
|---|
| [statistics(String[])](statistics.md) |

[Kraken API]\
open fun [statistics](statistics.md)(values: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)&gt;): [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)

Determine, and create a String with the bit set statistics. The statistics include: Size, Length, Cardinality, Total words (*i.e.*, the total number of 64-bit &quot;words&quot;), Set array length (*i.e.*, the number of references that can be held by the top level array, Level2 areas in use, Level3 blocks in use,, Level2 pool size, Level3 pool size, and the Compaction count. 

 This method is intended for diagnostic use (as it is relatively expensive in time), but can be useful in understanding an application's use of a `SparseBitSet`.

#### Return

a String detailing the statistics of the bit set

#### Since

1.6

#### Parameters

Kraken API

| | |
|---|---|
| values | an array for the individual results (if not null) |
