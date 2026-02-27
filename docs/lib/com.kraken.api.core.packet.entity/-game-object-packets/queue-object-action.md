//[lib](../../../index.md)/[com.kraken.api.core.packet.entity](../index.md)/[GameObjectPackets](index.md)/[queueObjectAction](queue-object-action.md)

# queueObjectAction

[Kraken API]\
open fun [queueObjectAction](queue-object-action.md)(actionFieldNo: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), objectId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), worldPointX: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), worldPointY: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), ctrlDown: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html))

Queues the low-level packet to perform a generic action click on a tile object. 

 This method sends one of the `OPLOC` packets (e.g., `OPLOC1` through `OPLOC10`), where the action is determined by the `actionFieldNo`.

#### Parameters

Kraken API

| | |
|---|---|
| actionFieldNo | The 1-based index of the action to execute (1-10). |
| objectId | The ID of the target object. |
| worldPointX | The X coordinate of the object's location in the world. |
| worldPointY | The Y coordinate of the object's location in the world. |
| ctrlDown | If true, indicates the control key was held down. |

[Kraken API]\
open fun [queueObjectAction](queue-object-action.md)(object: TileObject, ctrlDown: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html), actionlist: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)&gt;)

Queues a tile object action by matching a human-readable action string (e.g., &quot;Chop&quot;, &quot;Open&quot;, &quot;Bank&quot;). 

 This is a high-level convenience method that determines the target object's world coordinates, checks the object's available actions, finds a match for `actionlist`, and sends the correct low-level `OPLOC` packet.

#### Parameters

Kraken API

| | |
|---|---|
| object | The target TileObject (e.g., GameObject, WallObject). |
| ctrlDown | If true, indicates the control key was held down. |
| actionlist | A varargs list of action strings to search for (case-insensitive). |
