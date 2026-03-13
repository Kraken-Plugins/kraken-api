//[kraken-api](../../../index.md)/[com.kraken.api.service.pathfinding](../index.md)/[SparseBitSet](index.md)/[toStringCompaction](to-string-compaction.md)

# toStringCompaction

[Kraken API]\
open fun [toStringCompaction](to-string-compaction.md)(count: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html))

Sequences of set bits longer than this value are shown by [toString](to-string.md) as a &quot;sub-sequence,&quot; in the form `a..b`. Setting this value to zero causes each set bit to be listed individually. The default default value is 2 (which means sequences of three or more bits set are shown as a subsequence, and all other set bits are listed individually). 

 Note: this value will be passed to `SparseBitSet`s that may be created within or as a result of the operations on this bit set, or, for static methods, from the value belonging to the first parameter.

#### Since

1.6

#### Parameters

Kraken API

| | |
|---|---|
| count | the maximum count of a run of bits that are shown as individual entries in a `toString`() conversion. If 0, all bits are shown individually. |

#### See also

| |
|---|
| [toString()](to-string.md) |

[Kraken API]\
open fun [toStringCompaction](to-string-compaction.md)(change: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html))

If *change* is `true`, the current value of the *toStringCompaction*() value is made the default value for all `SparseBitSet`s created from this point onward in this JVM.

#### Since

1.6

#### Parameters

Kraken API

| | |
|---|---|
| change | if true, change the default value |
