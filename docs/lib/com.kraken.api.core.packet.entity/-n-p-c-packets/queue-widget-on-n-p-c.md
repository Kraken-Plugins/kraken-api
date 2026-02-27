//[lib](../../../index.md)/[com.kraken.api.core.packet.entity](../index.md)/[NPCPackets](index.md)/[queueWidgetOnNPC](queue-widget-on-n-p-c.md)

# queueWidgetOnNPC

[Kraken API]\
open fun [queueWidgetOnNPC](queue-widget-on-n-p-c.md)(npcIndex: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), sourceItemId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), sourceSlot: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), sourceWidgetId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), ctrlDown: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html))

Queues the raw packet for using a widget (typically an item) on an NPC. 

 This method sends the `OPNPCT` (Use Widget on NPC) packet, which contains the details of the source item/widget and the target NPC.

#### Parameters

Kraken API

| | |
|---|---|
| npcIndex | The server index of the target NPC. |
| sourceItemId | The ID of the item being used. |
| sourceSlot | The slot index of the item being used (e.g., inventory slot). |
| sourceWidgetId | The ID of the parent widget containing the item (e.g., inventory widget ID). |
| ctrlDown | If true, indicates the control key was held down. |

[Kraken API]\
open fun [queueWidgetOnNPC](queue-widget-on-n-p-c.md)(npc: NPC, widget: Widget)

Queues the packet for using a specific Widget (or item it represents) on a target NPC. 

 This is a convenience method that extracts the necessary item and widget details from the provided Widget object and calls the raw `queueWidgetOnNPC` method.

#### Parameters

Kraken API

| | |
|---|---|
| npc | The target NPC object. |
| widget | The source Widget containing the item or action to be used on the NPC. |
