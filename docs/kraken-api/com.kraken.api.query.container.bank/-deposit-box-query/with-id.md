//[kraken-api](../../../index.md)/[com.kraken.api.query.container.bank](../index.md)/[DepositBoxQuery](index.md)/[withId](with-id.md)

# withId

[Kraken API]\
open fun [withId](with-id.md)(id: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [DepositBoxQuery](index.md)

Filters for items in the deposit box that have the specified item id. 

This method is used to refine the `DepositBoxQuery` by including only items within the deposit box that match the provided item id. It applies a filtering condition to the query and returns the updated query object.

#### Return

DepositBoxQuery The updated `DepositBoxQuery` instance with the specified filter applied.

#### Parameters

Kraken API

| | |
|---|---|
| id | The item id to filter for. <br>- The id must represent a valid item within the deposit box. - If the id does not correspond to any item, the result will be an empty query. |
