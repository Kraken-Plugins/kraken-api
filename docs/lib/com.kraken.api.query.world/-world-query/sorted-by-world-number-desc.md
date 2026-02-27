//[lib](../../../index.md)/[com.kraken.api.query.world](../index.md)/[WorldQuery](index.md)/[sortedByWorldNumberDesc](sorted-by-world-number-desc.md)

# sortedByWorldNumberDesc

[Kraken API]\
open fun [sortedByWorldNumberDesc](sorted-by-world-number-desc.md)(): [WorldQuery](index.md)

Sorts the current query's worlds by their world numbers in descending order. 

 This method modifies the query to sort the worlds such that the worlds with higher world numbers appear earlier in the resulting sequence. The comparison is based on the @getId method of each world, which retrieves the world's numerical ID. 

#### Return

A `WorldQuery` object sorted by world numbers in descending order.
