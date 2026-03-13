//[kraken-api](../../../index.md)/[com.kraken.api.query.container.inventory](../index.md)/[InventoryQuery](index.md)/[hasItem](has-item.md)

# hasItem

[Kraken API]\
open fun [hasItem](has-item.md)(id: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Returns true when the inventory contains a specific item, found by its item id.

#### Return

True if the inventory has the item and false otherwise

#### Parameters

Kraken API

| | |
|---|---|
| id | The id of the item to search for |

[Kraken API]\
open fun [hasItem](has-item.md)(name: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Returns true when the inventory contains a specific item, found by its name. This is case-insensitive but does require the entire item name.

#### Return

True if the inventory has the item and false otherwise

#### Parameters

Kraken API

| | |
|---|---|
| name | The name of the item to search for |
