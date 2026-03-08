//[lib](../../../index.md)/[com.kraken.api.service.pathfinding](../index.md)/[GlobalPathfinder](index.md)/[findPath](find-path.md)

# findPath

[Kraken API]\
open fun [findPath](find-path.md)(start: WorldPoint, target: WorldPoint): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;

Finds a dense global path between two world points using A* and global collision data.

#### Return

A dense path that includes the start and target tiles, or an empty list when no path is found.

#### Parameters

Kraken API

| | |
|---|---|
| start | The starting tile. |
| target | The destination tile. |

[Kraken API]\
open fun [findPath](find-path.md)(start: WorldPoint, target: WorldPoint, maxExpandedNodes: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;

Finds a dense global path between two world points using A* and global collision data.

#### Return

A dense path that includes the start and target tiles, or an empty list when no path is found.

#### Parameters

Kraken API

| | |
|---|---|
| start | The starting tile. |
| target | The destination tile. |
| maxExpandedNodes | Maximum number of expanded nodes before aborting search. |
