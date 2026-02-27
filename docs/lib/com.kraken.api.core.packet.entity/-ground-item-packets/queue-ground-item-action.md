//[lib](../../../index.md)/[com.kraken.api.core.packet.entity](../index.md)/[GroundItemPackets](index.md)/[queueGroundItemAction](queue-ground-item-action.md)

# queueGroundItemAction

[Kraken API]\
open fun [queueGroundItemAction](queue-ground-item-action.md)(actionFieldNo: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), objectId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), worldPointX: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), worldPointY: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), ctrlDown: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html))

Queues the low-level packet to perform a generic action click on a ground item. 

 This method sends one of the `OPOBJ` packets (e.g., `OPOBJ1` through `OPOBJ5`, depending on the packet factory implementation). These packets are used for actions like &quot;Take,&quot; &quot;Examine,&quot; or other option clicks on a ground item.

#### Parameters

Kraken API

| | |
|---|---|
| actionFieldNo | The 1-based index of the action to execute (typically 1-5 for ground items). |
| objectId | The Item ID of the ground item. |
| worldPointX | The X coordinate of the item's location in the world. |
| worldPointY | The Y coordinate of the item's location in the world. |
| ctrlDown | If true, indicates the control key was held down. |

[Kraken API]\
open fun [queueGroundItemAction](queue-ground-item-action.md)(item: TileItem, location: WorldPoint, ctrlDown: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html))

Queues the packet to perform the default &quot;Take&quot; action (Action 3) on a ground item. 

 This is a convenience method for the most common interaction with a ground item. It uses action index 3, which is conventionally the &quot;Take&quot; option for items.

#### Parameters

Kraken API

| | |
|---|---|
| item | The target TileItem object. |
| location | The WorldPoint location of the item on the map. |
| ctrlDown | If true, indicates the control key was held down. |
