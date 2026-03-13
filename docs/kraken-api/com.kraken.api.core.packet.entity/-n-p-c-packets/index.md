//[kraken-api](../../../index.md)/[com.kraken.api.core.packet.entity](../index.md)/[NPCPackets](index.md)

# NPCPackets

[Kraken API]\
open class [NPCPackets](index.md)

A static utility class for sending packets related to Non-Player Character (NPC) interactions to the game server. 

 This class handles various forms of NPC interaction, including standard action clicks (e.g., Talk-to, Attack) and &quot;use-with&quot; actions (e.g., using an item on an NPC). It uses a [PacketDefFactory](../../com.kraken.api.core.packet.model/-packet-def-factory/index.md) to determine the correct packet type and a [PacketClient](../../com.kraken.api.core.packet/-packet-client/index.md) to send the raw data.

## Constructors

| | |
|---|---|
| [NPCPackets](-n-p-c-packets.md) | [Kraken API]<br>constructor() |

## Functions

| Name | Summary |
|---|---|
| [queueNPCAction](queue-n-p-c-action.md) | [Kraken API]<br>open fun [queueNPCAction](queue-n-p-c-action.md)(npc: NPC, actionList: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)&gt;)<br>Queues an NPC action by matching a human-readable action string (e.g., &quot;Talk-to&quot;, &quot;Attack&quot;).<br>[Kraken API]<br>open fun [queueNPCAction](queue-n-p-c-action.md)(actionFieldNo: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), npcIndex: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), ctrlDown: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html))<br>Queues the low-level packet to perform a generic action click on an NPC. |
| [queueWidgetOnNPC](queue-widget-on-n-p-c.md) | [Kraken API]<br>open fun [queueWidgetOnNPC](queue-widget-on-n-p-c.md)(npc: NPC, widget: Widget)<br>Queues the packet for using a specific Widget (or item it represents) on a target NPC.<br>[Kraken API]<br>open fun [queueWidgetOnNPC](queue-widget-on-n-p-c.md)(npcIndex: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), sourceItemId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), sourceSlot: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), sourceWidgetId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), ctrlDown: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html))<br>Queues the raw packet for using a widget (typically an item) on an NPC. |
