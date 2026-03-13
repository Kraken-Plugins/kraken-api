//[kraken-api](../../../index.md)/[com.kraken.api.core.packet.entity](../index.md)/[PlayerPackets](index.md)

# PlayerPackets

[Kraken API]\
open class [PlayerPackets](index.md)

A high-level utility class for sending packets related to Player interactions to the game server. 

 This class handles actions like right-clicking other players (e.g., Follow, Trade) and &quot;use-on&quot; actions (e.g., using an item on a player). It abstracts the low-level packet construction and uses client data to determine the correct action index.

## Constructors

| | |
|---|---|
| [PlayerPackets](-player-packets.md) | [Kraken API]<br>constructor() |

## Functions

| Name | Summary |
|---|---|
| [queuePlayerAction](queue-player-action.md) | [Kraken API]<br>open fun [queuePlayerAction](queue-player-action.md)(player: Player, actionlist: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)&gt;)<br>Queues a player action by matching a human-readable action string (e.g., &quot;Attack&quot;, &quot;Trade&quot;, &quot;Follow&quot;).<br>[Kraken API]<br>open fun [queuePlayerAction](queue-player-action.md)(actionFieldNo: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), playerIndex: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), ctrlDown: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html))<br>Queues the low-level packet to perform a generic action click on another player. |
| [queueWidgetOnPlayer](queue-widget-on-player.md) | [Kraken API]<br>open fun [queueWidgetOnPlayer](queue-widget-on-player.md)(player: Player, widget: Widget)<br>Queues the packet for using a specific Widget (or item it represents) on a target Player.<br>[Kraken API]<br>open fun [queueWidgetOnPlayer](queue-widget-on-player.md)(playerIndex: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), sourceItemId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), sourceSlot: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), sourceWidgetId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), ctrlDown: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html))<br>Queues the raw packet for using a widget (typically an item) on another player. |
