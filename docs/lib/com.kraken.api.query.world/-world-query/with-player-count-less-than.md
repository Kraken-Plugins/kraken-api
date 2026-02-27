//[lib](../../../index.md)/[com.kraken.api.query.world](../index.md)/[WorldQuery](index.md)/[withPlayerCountLessThan](with-player-count-less-than.md)

# withPlayerCountLessThan

[Kraken API]\
open fun [withPlayerCountLessThan](with-player-count-less-than.md)(count: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [WorldQuery](index.md)

Filters the current query to include only worlds with a player count less than the specified value. 

 This method modifies the query to exclude any worlds where the player count is greater than or equal to the provided `count` value. 

#### Return

A `WorldQuery` object filtered to include only worlds with a player count less than the specified value.

#### Parameters

Kraken API

| | |
|---|---|
| count | The maximum player count (exclusive) to include in the filtered results. Only worlds with a player count less than this value will be included. |
