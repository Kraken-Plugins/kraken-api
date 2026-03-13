//[kraken-api](../../../index.md)/[com.kraken.api.query.world](../index.md)/[WorldQuery](index.md)/[withOutTypes](with-out-types.md)

# withOutTypes

[Kraken API]\
open fun [withOutTypes](with-out-types.md)(types: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;WorldType&gt;): [WorldQuery](index.md)

Filters the query to exclude worlds that contain any of the specified types. 

This method ensures that the resulting query does not include any worlds where the types match those provided in the `types` parameter.

#### Return

a @WorldQuery instance with the applied filter to exclude the specified types.

#### Parameters

Kraken API

| | |
|---|---|
| types | an array of @WorldType that specifies the types to exclude from the query. If `null` or empty, no types will be excluded. |
