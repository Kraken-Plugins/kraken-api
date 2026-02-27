//[lib](../../../index.md)/[com.kraken.api.service.ui.processing](../index.md)/[ProcessingService](index.md)/[process](process.md)

# process

[Kraken API]\
open fun [process](process.md)(itemIds: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)&gt;): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Confirms the selection of one of the specified item IDs by resuming the appropriate widget interaction based on the current multi-quantity value. This method expects the item id of the item to create, not necessarily the item the player has. i.e. for cooking Salmon it expects the item id of a cooked salmon, not the raw salmon that the player may have in their inventory. 

This method iterates over a map of processable item IDs and their associated slot indices, checking if any of the provided `itemIds` match the available items. If a match is found, it sends a &quot;resume/pause&quot; action packet for the corresponding widget slot with the current quantity value.

#### Return

`true` if at least one of the provided `itemIds` matches the processable items and an interaction is successfully queued; `false` otherwise.

#### Parameters

Kraken API

| | |
|---|---|
| itemIds | A variable-length list of item IDs to compare against the processable items currently available. These represent the items the user wants to confirm. |

[Kraken API]\
open fun [process](process.md)(containerItem: [ContainerItem](../../com.kraken.api.query.container/-container-item/index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Confirms the selection of one of the specified container items by resuming the appropriate widget interaction based on the current multi-quantity value. This method expects the item id of the item to create, not necessarily the item the player has. i.e. for cooking Salmon it expects the item id of a cooked salmon, not the raw salmon that the player may have in their inventory. 

This method iterates over a map of processable item IDs and their associated slot indices, checking if any of the provided `itemIds` match the available items. If a match is found, it sends a &quot;resume/pause&quot; action packet for the corresponding widget slot with the current quantity value.

#### Return

`true` if at least one of the provided `itemIds` matches the processable items and an interaction is successfully queued; `false` otherwise.

#### Parameters

Kraken API

| | |
|---|---|
| containerItem | A non null container item to compare against the processable items currently available. These represent the items the user wants to confirm. |

[Kraken API]\
open fun [process](process.md)(itemNames: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)&gt;): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Confirms the selection of one of the specified item names by resuming the appropriate widget interaction based on the current multi-quantity value. 

This method iterates over a map of processable item IDs and their associated slot indices, checking if any of the provided `itemNames` match the available items. If a match is found, it sends a &quot;resume/pause&quot; action packet for the corresponding widget slot with the current quantity value.

#### Return

`true` if at least one of the provided `itemNames` matches the processable items and an interaction is successfully queued; `false` otherwise.

#### Parameters

Kraken API

| | |
|---|---|
| itemNames | A variable-length list of item names to compare against the processable items currently available. These represent the items the user wants to confirm. |
