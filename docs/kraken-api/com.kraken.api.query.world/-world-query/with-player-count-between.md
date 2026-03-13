//[kraken-api](../../../index.md)/[com.kraken.api.query.world](../index.md)/[WorldQuery](index.md)/[withPlayerCountBetween](with-player-count-between.md)

# withPlayerCountBetween

[Kraken API]\
open fun [withPlayerCountBetween](with-player-count-between.md)(min: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), max: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [WorldQuery](index.md)

Filters the current query to include only worlds with a player count within the specified range. 

 This method modifies the query to include only worlds where the player count falls between the given minimum and maximum values (inclusive). Worlds with player counts outside this range will be excluded from the results. 

#### Return

A `WorldQuery` object filtered to include only worlds with player counts within the specified range.

#### Parameters

Kraken API

| | |
|---|---|
| min | The minimum player count (inclusive) to include in the filtered results. |
| max | The maximum player count (inclusive) to include in the filtered results. |
