//[lib](../../../index.md)/[com.kraken.api.query.world](../index.md)/[WorldQuery](index.md)/[inRegion](in-region.md)

# inRegion

[Kraken API]\
open fun [inRegion](in-region.md)(region: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;WorldRegion&gt;): [WorldQuery](index.md)

Filters the current query to include only worlds that belong to the specified regions. 

 This method modifies the query to match worlds where their associated region matches any of the provided `WorldRegion` values. If the world's region is `null`, it will be excluded from the filtered results. 

#### Return

A `WorldQuery` object filtered to include only worlds belonging to the specified regions.

#### Parameters

Kraken API

| | |
|---|---|
| region | An array of `WorldRegion` values to filter worlds by. Only worlds belonging to one or more of the specified regions will be included in the results. |
