//[lib](../../../index.md)/[com.kraken.api.query.world](../index.md)/[WorldQuery](index.md)/[withTypes](with-types.md)

# withTypes

[Kraken API]\
open fun [withTypes](with-types.md)(types: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;WorldType&gt;): [WorldQuery](index.md)

Filters the current query to include only worlds that match any of the specified `WorldType` values. 

 This method modifies the query to include worlds where their list of types contains at least one of the provided `WorldType` values. If the world's types are `null`, it will be excluded from the filtered results. 

#### Return

A `WorldQuery` object filtered to include only worlds matching the specified types.

#### Parameters

Kraken API

| | |
|---|---|
| types | An array of `WorldType` values to filter worlds by. Only worlds containing one or more of the specified types will be included in the results. |
