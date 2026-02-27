//[lib](../../../index.md)/[com.kraken.api.core.packet.entity](../index.md)/[GroundItemPackets](index.md)/[queueWidgetOnGroundItem](queue-widget-on-ground-item.md)

# queueWidgetOnGroundItem

[Kraken API]\
open fun [queueWidgetOnGroundItem](queue-widget-on-ground-item.md)(objectId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), worldPointX: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), worldPointY: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), sourceSlot: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), sourceItemId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), sourceWidgetId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), ctrlDown: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html))

Queues the raw packet for using a widget (typically an item) on a ground item. 

 This method sends the `OPOBJT` (Use Widget on Object/Item) packet, which includes the coordinates of the target ground item and the details of the source item/widget.

#### Parameters

Kraken API

| | |
|---|---|
| objectId | The Item ID of the ground item. |
| worldPointX | The X coordinate of the item's location in the world. |
| worldPointY | The Y coordinate of the item's location in the world. |
| sourceSlot | The slot index of the item being used (e.g., inventory slot). |
| sourceItemId | The ID of the item being used. |
| sourceWidgetId | The ID of the parent widget containing the item (e.g., inventory widget ID). |
| ctrlDown | If true, indicates the control key was held down. |

[Kraken API]\
open fun [queueWidgetOnGroundItem](queue-widget-on-ground-item.md)(item: TileItem, location: WorldPoint, w: Widget, ctrlDown: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html))

Queues the packet for using a specific Widget (or item it represents) on a target TileItem. 

 This is a convenience method that simplifies sending the `OPOBJT` packet by extracting necessary details from the provided TileItem and source Widget.

#### Parameters

Kraken API

| | |
|---|---|
| item | The target TileItem object on the ground. |
| location | The WorldPoint location of the item on the map. |
| w | The source Widget containing the item or action to be used on the ground item. |
| ctrlDown | If true, indicates the control key was held down. |
