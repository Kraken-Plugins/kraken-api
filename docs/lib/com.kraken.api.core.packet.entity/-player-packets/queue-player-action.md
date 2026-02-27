//[lib](../../../index.md)/[com.kraken.api.core.packet.entity](../index.md)/[PlayerPackets](index.md)/[queuePlayerAction](queue-player-action.md)

# queuePlayerAction

[Kraken API]\
open fun [queuePlayerAction](queue-player-action.md)(actionFieldNo: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), playerIndex: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), ctrlDown: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html))

Queues the low-level packet to perform a generic action click on another player. 

 This method sends one of the `OPPLAYER` packets (e.g., `OPPLAYER1` through `OPPLAYER10`), where the action is determined by the `actionFieldNo` (which corresponds to a Player Option).

#### Parameters

Kraken API

| | |
|---|---|
| actionFieldNo | The 1-based index of the action to execute (1-10). |
| playerIndex | The server index/ID of the target player. |
| ctrlDown | If true, indicates the control key was held down. |

[Kraken API]\
open fun [queuePlayerAction](queue-player-action.md)(player: Player, actionlist: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)&gt;)

Queues a player action by matching a human-readable action string (e.g., &quot;Attack&quot;, &quot;Trade&quot;, &quot;Follow&quot;). 

 This is a high-level convenience method that checks the client's current **Player Options** (the text that appears on the right-click menu) for a matching action, finds the corresponding action number (1-10), and sends the correct packet.

#### Parameters

Kraken API

| | |
|---|---|
| player | The target Player object to interact with. |
| actionlist | A varargs list of action strings to search for (case-insensitive). |
