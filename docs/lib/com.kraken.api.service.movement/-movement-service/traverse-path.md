//[lib](../../../index.md)/[com.kraken.api.service.movement](../index.md)/[MovementService](index.md)/[traversePath](traverse-path.md)

# traversePath

[Kraken API]\
open fun [traversePath](traverse-path.md)(client: Client, path: [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Traverses a given path made up of waypoints, attempting to successfully move the player to each WorldPoint in the sequence. This method moves the player towards the target waypoints and handles retries for unreachable points. The traversal stops if any waypoint cannot be reached after multiple attempts.

#### Return

`true` if the path was successfully traversed to the end, or `false` if any waypoint could not be reached after retries.

#### Parameters

Kraken API

| | |
|---|---|
| client | The client instance used to interact with the game world and manage player movement. |
| path | A list of WorldPoint objects representing the waypoints to traverse in sequence. |

[Kraken API]\
open fun [traversePath](traverse-path.md)(client: Client, path: [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;, onWaypointReached: [Consumer](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/function/Consumer.html)&lt;[String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)&gt;): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Traverses a given path made up of waypoints, attempting to successfully move the player to each WorldPoint in the sequence. This method moves the player towards the target waypoints and handles retries for unreachable points. The traversal stops if any waypoint cannot be reached after multiple attempts.

#### Return

`true` if the path was successfully traversed to the end, or `false` if any waypoint could not be reached after retries.

#### Parameters

Kraken API

| | |
|---|---|
| client | The client instance used to interact with the game world and manage player movement. |
| path | A list of WorldPoint objects representing the waypoints to traverse in sequence. |
| onWaypointReached | A functional interface invoked when a waypoint in the path is reached |

[Kraken API]\
open fun [traversePath](traverse-path.md)(client: Client, path: [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;, onWaypointReached: [Consumer](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/function/Consumer.html)&lt;[String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)&gt;, onDestinationReached: [Consumer](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/function/Consumer.html)&lt;[String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)&gt;): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Traverses a given path made up of waypoints, attempting to successfully move the player to each WorldPoint in the sequence. This method invokes movement commands and uses retries if a waypoint fails to be reached. It aborts if a waypoint cannot be reached after multiple attempts. 

The method performs the following tasks for each waypoint in the path: 

- Sends a movement command to the client to move towards the target waypoint.
- Calculates a dynamic timeout based on distance and walking speed, with a buffer for path variance.
- Waits for the player to reach the waypoint within the allowed timeout.
- Retries the movement command up to two times if the waypoint is not reached within the timeout.
- Aborts and returns failure if retries are exhausted for any waypoint.

#### Return

`true` if the path was successfully traversed to the end, `false` if any waypoint could not be reached after retries.

#### Parameters

Kraken API

| | |
|---|---|
| client | The client instance used to interact with the game world and retrieve the player's location. |
| path | A list of WorldPoint objects representing the sequence of waypoints to traverse. |
| onWaypointReached | A functional interface invoked when a waypoint is reached |
| onDestinationReached | A functional interface invoked when the paths final destination is reached |
