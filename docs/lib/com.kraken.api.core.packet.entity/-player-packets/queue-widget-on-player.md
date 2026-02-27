//[lib](../../../index.md)/[com.kraken.api.core.packet.entity](../index.md)/[PlayerPackets](index.md)/[queueWidgetOnPlayer](queue-widget-on-player.md)

# queueWidgetOnPlayer

[Kraken API]\
open fun [queueWidgetOnPlayer](queue-widget-on-player.md)(playerIndex: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), sourceItemId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), sourceSlot: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), sourceWidgetId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), ctrlDown: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html))

Queues the raw packet for using a widget (typically an item) on another player. 

 This method sends the `OPPLAYERT` (Use Widget on Player) packet, which contains the details of the source item/widget and the target player.

#### Parameters

Kraken API

| | |
|---|---|
| playerIndex | The server index/ID of the target player. |
| sourceItemId | The ID of the item being used. |
| sourceSlot | The slot index of the item being used (e.g., inventory slot). |
| sourceWidgetId | The ID of the parent widget containing the item (e.g., inventory widget ID). |
| ctrlDown | If true, indicates the control key was held down. |

[Kraken API]\
open fun [queueWidgetOnPlayer](queue-widget-on-player.md)(player: Player, widget: Widget)

Queues the packet for using a specific Widget (or item it represents) on a target Player. 

 This is a convenience method that extracts the necessary item and widget details from the provided Widget object and calls the raw `queueWidgetOnPlayer` method.

#### Parameters

Kraken API

| | |
|---|---|
| player | The target Player object. |
| widget | The source Widget containing the item or action to be used on the player. |
