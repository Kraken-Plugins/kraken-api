//[lib](../../../../index.md)/[com.kraken.api.service.pathfinding](../../index.md)/[SparseBitSet](../index.md)/[Statistics](index.md)

# Statistics

enum [Statistics](index.md)

These enumeration values are used as labels for the values in the String created by the *statistics*() method. The values of the corresponding statistics are `int`s, except for the loadFactor and Average_chain_length values, which are `float`s. 

 An array of `String`s may be obtained containing a representation of each of these values. An element of such an array, say, `values`, may be accessed, for example, by: 

```kotlin
values[SparseBitSet.statistics.Buckets_available.ordinal()]
```

#### See also

| |
|---|
| [statistics(String[])](../statistics.md) |

## Entries

| | |
|---|---|
| [Size](-size/index.md) | [Kraken API]<br>[Size](-size/index.md)<br>The size of the bit set, as give by the *size*() method. |
| [Length](-length/index.md) | [Kraken API]<br>[Length](-length/index.md)<br>The length of the bit set, as give by the *length*() method. |
| [Cardinality](-cardinality/index.md) | [Kraken API]<br>[Cardinality](-cardinality/index.md)<br>The cardinality of the bit set, as give by the *cardinality*() method. |
| [Total_words](-total_words/index.md) | [Kraken API]<br>[Total_words](-total_words/index.md)<br>The total number of non-zero 64-bits &quot;words&quot; being used to hold the representation of the bit set. |
| [Set_array_length](-set_array_length/index.md) | [Kraken API]<br>[Set_array_length](-set_array_length/index.md)<br>The length of the bit set array. |
| [Set_array_max_length](-set_array_max_length/index.md) | [Kraken API]<br>[Set_array_max_length](-set_array_max_length/index.md)<br>The maximum permitted length of the bit set array. |
| [Level2_areas](-level2_areas/index.md) | [Kraken API]<br>[Level2_areas](-level2_areas/index.md)<br>The number of level2 areas. |
| [Level2_area_length](-level2_area_length/index.md) | [Kraken API]<br>[Level2_area_length](-level2_area_length/index.md)<br>The length of the level2 areas. |
| [Level3_blocks](-level3_blocks/index.md) | [Kraken API]<br>[Level3_blocks](-level3_blocks/index.md)<br>The total number of level3 blocks in use. |
| [Level3_block_length](-level3_block_length/index.md) | [Kraken API]<br>[Level3_block_length](-level3_block_length/index.md)<br>The length of the level3 blocks. |
| [Compaction_count_value](-compaction_count_value/index.md) | [Kraken API]<br>[Compaction_count_value](-compaction_count_value/index.md)<br>Is the value that determines how the *toString*() conversion is performed. |

## Functions

| Name | Summary |
|---|---|
| [valueOf](value-of.md) | [Kraken API]<br>open fun [valueOf](value-of.md)(name: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)): [SparseBitSet.Statistics](index.md)<br>Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.) |
| [values](values.md) | [Kraken API]<br>open fun [values](values.md)(): [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[SparseBitSet.Statistics](index.md)&gt;<br>Returns an array containing the constants of this enum type, in the order they're declared. This method may be used to iterate over the constants. |
