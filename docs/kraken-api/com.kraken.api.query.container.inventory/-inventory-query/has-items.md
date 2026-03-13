//[kraken-api](../../../index.md)/[com.kraken.api.query.container.inventory](../index.md)/[InventoryQuery](index.md)/[hasItems](has-items.md)

# hasItems

[Kraken API]\
open fun [hasItems](has-items.md)(ids: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)&gt;): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Returns true ONLY if the inventory contains ALL of the specified item IDs.

#### Return

True if every single ID in the arguments exists in the inventory.

#### Parameters

Kraken API

| | |
|---|---|
| ids | Variable argument of item IDs to search for. |

[Kraken API]\
open fun [hasItems](has-items.md)(ids: [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;[Integer](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Integer.html)&gt;): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Determines whether the inventory contains all items specified by the given list of IDs. 

 If the provided list of IDs is `null` or empty, the method returns `true`. Otherwise, it internally converts the list to an array and delegates the check to the `hasItems(int... ids)` method.

#### Return

`true` if the inventory contains all items specified in the list, or if the list is `null` or empty. Otherwise, `false` is returned.

#### Parameters

Kraken API

| | |
|---|---|
| ids | A `List` of `Integer` IDs representing the items to search for. Each ID corresponds to a specific inventory item. <br>- If the list is `null` or empty, the method will return `true`. - All IDs in the list must exist in the inventory for the method to return `true`. |

[Kraken API]\
open fun [hasItems](has-items.md)(names: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)&gt;): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Returns true ONLY if the inventory contains ALL of the specified item names. This is case-insensitive.

#### Return

True if every single name in the arguments exists in the inventory.

#### Parameters

Kraken API

| | |
|---|---|
| names | Variable argument of item names to search for. |
