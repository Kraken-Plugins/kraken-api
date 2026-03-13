//[kraken-api](../../../index.md)/[com.kraken.api.core.packet.entity](../index.md)/[GroundItemPackets](index.md)

# GroundItemPackets

[Kraken API]\
open class [GroundItemPackets](index.md)

A utility class for sending packets related to TileItem (ground item) interactions to the game server. 

 This class primarily handles picking up ground items (actions) and using items/widgets directly on a ground item. It abstracts the low-level packet construction and world coordinate handling.

## Constructors

| | |
|---|---|
| [GroundItemPackets](-ground-item-packets.md) | [Kraken API]<br>constructor() |

## Functions

| Name | Summary |
|---|---|
| [queueGroundItemAction](queue-ground-item-action.md) | [Kraken API]<br>open fun [queueGroundItemAction](queue-ground-item-action.md)(item: TileItem, location: WorldPoint, ctrlDown: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html))<br>Queues the packet to perform the default &quot;Take&quot; action (Action 3) on a ground item.<br>[Kraken API]<br>open fun [queueGroundItemAction](queue-ground-item-action.md)(actionFieldNo: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), objectId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), worldPointX: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), worldPointY: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), ctrlDown: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html))<br>Queues the low-level packet to perform a generic action click on a ground item. |
| [queueWidgetOnGroundItem](queue-widget-on-ground-item.md) | [Kraken API]<br>open fun [queueWidgetOnGroundItem](queue-widget-on-ground-item.md)(item: TileItem, location: WorldPoint, w: Widget, ctrlDown: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html))<br>Queues the packet for using a specific Widget (or item it represents) on a target TileItem.<br>[Kraken API]<br>open fun [queueWidgetOnGroundItem](queue-widget-on-ground-item.md)(objectId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), worldPointX: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), worldPointY: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), sourceSlot: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), sourceItemId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), sourceWidgetId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), ctrlDown: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html))<br>Queues the raw packet for using a widget (typically an item) on a ground item. |
