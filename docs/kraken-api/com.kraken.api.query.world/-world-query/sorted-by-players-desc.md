//[kraken-api](../../../index.md)/[com.kraken.api.query.world](../index.md)/[WorldQuery](index.md)/[sortedByPlayersDesc](sorted-by-players-desc.md)

# sortedByPlayersDesc

[Kraken API]\
open fun [sortedByPlayersDesc](sorted-by-players-desc.md)(): [WorldQuery](index.md)

Sorts the current query's worlds by their player counts in descending order. 

 This method modifies the query to sort the worlds such that the worlds with higher player counts appear earlier in the resulting sequence. The comparison is based on the raw player count value retrieved for each world. 

#### Return

A `WorldQuery` object sorted by player counts in descending order.
