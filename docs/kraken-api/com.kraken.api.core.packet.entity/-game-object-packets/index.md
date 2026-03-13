//[kraken-api](../../../index.md)/[com.kraken.api.core.packet.entity](../index.md)/[GameObjectPackets](index.md)

# GameObjectPackets

[Kraken API]\
open class [GameObjectPackets](index.md)

A high-level utility class for sending packets related to TileObject (Game Object) interactions to the game server. 

 This class handles actions like clicking on doors, trees, or banks, as well as &quot;use-on&quot; actions (e.g., using an item on an object). It abstracts the low-level packet construction and world coordinate calculations.

## Constructors

| | |
|---|---|
| [GameObjectPackets](-game-object-packets.md) | [Kraken API]<br>constructor() |

## Functions

| Name | Summary |
|---|---|
| [queueObjectAction](queue-object-action.md) | [Kraken API]<br>open fun [queueObjectAction](queue-object-action.md)(object: TileObject, ctrlDown: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html), actionlist: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)&gt;)<br>Queues a tile object action by matching a human-readable action string (e.g., &quot;Chop&quot;, &quot;Open&quot;, &quot;Bank&quot;).<br>[Kraken API]<br>open fun [queueObjectAction](queue-object-action.md)(actionFieldNo: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), objectId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), worldPointX: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), worldPointY: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), ctrlDown: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html))<br>Queues the low-level packet to perform a generic action click on a tile object. |
| [queueWidgetOnTileObject](queue-widget-on-tile-object.md) | [Kraken API]<br>open fun [queueWidgetOnTileObject](queue-widget-on-tile-object.md)(widget: Widget, object: TileObject)<br>Queues the packet for using a specific Widget (or item it represents) on a target TileObject.<br>[Kraken API]<br>open fun [queueWidgetOnTileObject](queue-widget-on-tile-object.md)(objectId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), worldPointX: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), worldPointY: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), sourceSlot: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), sourceItemId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), sourceWidgetId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), ctrlDown: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html))<br>Queues the raw packet for using a widget (typically an item) on a tile object. |
