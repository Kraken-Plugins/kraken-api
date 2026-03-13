//[kraken-api](../../../index.md)/[com.kraken.api.service.movement](../index.md)/[MovementService](index.md)

# MovementService

[Kraken API]\
open class [MovementService](index.md)

## Constructors

| | |
|---|---|
| [MovementService](-movement-service.md) | [Kraken API]<br>constructor() |

## Functions

| Name | Summary |
|---|---|
| [applyVariableStride](apply-variable-stride.md) | [Kraken API]<br>open fun [applyVariableStride](apply-variable-stride.md)(densePath: [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;<br>open fun [applyVariableStride](apply-variable-stride.md)(densePath: [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;, config: [VariableStrideConfig](../-variable-stride-config/index.md)): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;<br>Converts a dense path (every tile) into a strided path of &quot;waypoint&quot; tiles. |
| [moveTo](move-to.md) | [Kraken API]<br>open fun [moveTo](move-to.md)(point: LocalPoint)<br>Moves the player to a specified LocalPoint.<br>[Kraken API]<br>open fun [moveTo](move-to.md)(point: WorldPoint)<br>Moves the player to the specified WorldPoint, handling instanced areas conversion when necessary. |
| [traversePath](traverse-path.md) | [Kraken API]<br>open fun [traversePath](traverse-path.md)(client: Client, path: [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>open fun [traversePath](traverse-path.md)(client: Client, path: [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;, onWaypointReached: [Consumer](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/function/Consumer.html)&lt;[String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)&gt;): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>open fun [traversePath](traverse-path.md)(client: Client, path: [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;, onWaypointReached: [Consumer](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/function/Consumer.html)&lt;[String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)&gt;, onDestinationReached: [Consumer](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/function/Consumer.html)&lt;[String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)&gt;): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Traverses a given path made up of waypoints, attempting to successfully move the player to each WorldPoint in the sequence. |
