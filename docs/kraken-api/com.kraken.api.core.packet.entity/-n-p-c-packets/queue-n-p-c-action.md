//[kraken-api](../../../index.md)/[com.kraken.api.core.packet.entity](../index.md)/[NPCPackets](index.md)/[queueNPCAction](queue-n-p-c-action.md)

# queueNPCAction

[Kraken API]\
open fun [queueNPCAction](queue-n-p-c-action.md)(actionFieldNo: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), npcIndex: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), ctrlDown: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html))

Queues the low-level packet to perform a generic action click on an NPC. 

 This method sends one of the `OPNPC` packets (e.g., `OPNPC1` through `OPNPC10`), where the action is determined by the `actionFieldNo`.

#### Parameters

Kraken API

| | |
|---|---|
| actionFieldNo | The 1-based index of the action to execute (1-10). |
| npcIndex | The server index of the target NPC. |
| ctrlDown | If true, indicates the control key was held down (often used for force-attacking/force-clicking). |

[Kraken API]\
open fun [queueNPCAction](queue-n-p-c-action.md)(npc: NPC, actionList: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)&gt;)

Queues an NPC action by matching a human-readable action string (e.g., &quot;Talk-to&quot;, &quot;Attack&quot;). 

 This is a high-level convenience method that inspects the target NPC's composition for a matching action and automatically determines the correct low-level action number (1-10) to use for the `OPNPC` packet. The search is case-insensitive.

#### Parameters

Kraken API

| | |
|---|---|
| npc | The target NPC object to interact with. |
| actionList | A varargs list of action strings to search for. The first match found will be executed. |
