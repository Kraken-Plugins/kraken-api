//[lib](../../../index.md)/[com.kraken.api.query.world](../index.md)/[WorldQuery](index.md)/[sortByWorldNumberAsc](sort-by-world-number-asc.md)

# sortByWorldNumberAsc

[Kraken API]\
open fun [sortByWorldNumberAsc](sort-by-world-number-asc.md)(): [WorldQuery](index.md)

Sorts the query results in ascending order based on the world number (ID). 

 This method arranges all queried World objects such that the objects with lower world numbers (IDs) appear before those with higher world numbers. 

**Note:** The comparison uses the Integer.compare method to determine the ordering of the world numbers.

#### Return

A `WorldQuery` object representing the current state of the query, sorted by world number in ascending order.
