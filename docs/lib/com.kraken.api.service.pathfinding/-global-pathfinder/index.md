//[lib](../../../index.md)/[com.kraken.api.service.pathfinding](../index.md)/[GlobalPathfinder](index.md)

# GlobalPathfinder

[Kraken API]\
open class [GlobalPathfinder](index.md)

## Constructors

| | |
|---|---|
| [GlobalPathfinder](-global-pathfinder.md) | [Kraken API]<br>constructor()<br>Constructs a global pathfinder instance and attempts to load the bundled global collision map. |

## Functions

| Name | Summary |
|---|---|
| [findPath](find-path.md) | [Kraken API]<br>open fun [findPath](find-path.md)(start: WorldPoint, target: WorldPoint): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;<br>open fun [findPath](find-path.md)(start: WorldPoint, target: WorldPoint, maxExpandedNodes: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;<br>Finds a dense global path between two world points using A* and global collision data. |
| [findPathWithBackoff](find-path-with-backoff.md) | [Kraken API]<br>open fun [findPathWithBackoff](find-path-with-backoff.md)(start: WorldPoint, target: WorldPoint, maxBackoffRadius: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;<br>Attempts to find a path to the target tile and progressively backs off around the target if needed. |
| [findSparsePath](find-sparse-path.md) | [Kraken API]<br>open fun [findSparsePath](find-sparse-path.md)(start: WorldPoint, target: WorldPoint): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;<br>Finds a sparse global path where waypoints are only kept when travel direction changes. |
| [isBlocked](is-blocked.md) | [Kraken API]<br>open fun [isBlocked](is-blocked.md)(worldPoint: WorldPoint): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Checks whether a tile is blocked in the global collision map. |
| [load](load.md) | [Kraken API]<br>open fun [load](load.md)(): [GlobalPathfinder](index.md)<br>Loads a global pathfinder from the bundled `/map.dat` collision map.<br>[Kraken API]<br>open fun [load](load.md)(filePath: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)): [GlobalPathfinder](index.md)<br>Loads a global pathfinder from a specific serialized map file path. |
| [toSparsePath](to-sparse-path.md) | [Kraken API]<br>open fun [toSparsePath](to-sparse-path.md)(densePath: [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;<br>Reduces a dense path into sparse waypoints by keeping tiles where direction changes. |
