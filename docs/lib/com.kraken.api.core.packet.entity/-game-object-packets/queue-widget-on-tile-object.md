//[lib](../../../index.md)/[com.kraken.api.core.packet.entity](../index.md)/[GameObjectPackets](index.md)/[queueWidgetOnTileObject](queue-widget-on-tile-object.md)

# queueWidgetOnTileObject

[Kraken API]\
open fun [queueWidgetOnTileObject](queue-widget-on-tile-object.md)(objectId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), worldPointX: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), worldPointY: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), sourceSlot: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), sourceItemId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), sourceWidgetId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), ctrlDown: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html))

Queues the raw packet for using a widget (typically an item) on a tile object. 

 This method sends the `OPLOCT` (Use Widget on Location/Object) packet, which includes the coordinates of the target object and the details of the source item/widget.

#### Parameters

Kraken API

| | |
|---|---|
| objectId | The ID of the target object. |
| worldPointX | The X coordinate of the object's location in the world. |
| worldPointY | The Y coordinate of the object's location in the world. |
| sourceSlot | The slot index of the item being used (e.g., inventory slot). |
| sourceItemId | The ID of the item being used. |
| sourceWidgetId | The ID of the parent widget containing the item (e.g., inventory widget ID). |
| ctrlDown | If true, indicates the control key was held down. |

[Kraken API]\
open fun [queueWidgetOnTileObject](queue-widget-on-tile-object.md)(widget: Widget, object: TileObject)

Queues the packet for using a specific Widget (or item it represents) on a target TileObject. 

 This is a convenience method that first calculates the target object's world coordinates and then calls the raw `queueWidgetOnTileObject` method with the source widget's details.

#### Parameters

Kraken API

| | |
|---|---|
| widget | The source Widget containing the item or action to be used on the object. |
| object | The target TileObject. |
