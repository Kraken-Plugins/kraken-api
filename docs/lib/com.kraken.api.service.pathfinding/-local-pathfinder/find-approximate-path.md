//[lib](../../../index.md)/[com.kraken.api.service.pathfinding](../index.md)/[LocalPathfinder](index.md)/[findApproximatePath](find-approximate-path.md)

# findApproximatePath

[Kraken API]\
open fun [findApproximatePath](find-approximate-path.md)(start: WorldPoint, target: WorldPoint): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;

Finds an approximate path to a random reachable tile within a default radius of 5 tiles around the target location.

#### Return

A list of WorldPoints representing the path to the approximate target.

#### Parameters

Kraken API

| | |
|---|---|
| start | The starting WorldPoint. |
| target | The target WorldPoint. |

[Kraken API]\
open fun [findApproximatePath](find-approximate-path.md)(start: WorldPoint, target: WorldPoint, radius: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;

Finds an approximate path to a random reachable tile within a specified radius around the target location. 

This method first calculates all reachable tiles from the start point using BFS. It then filters these tiles to find ones that lie within the specified square radius (Chebyshev distance) of the target point. Finally, it selects one of these candidates at random and computes a path to it.

#### Return

A list of WorldPoints representing the path to the approximate target.

#### Parameters

Kraken API

| | |
|---|---|
| start | The starting WorldPoint. |
| target | The target WorldPoint. |
| radius | The radius (in tiles) around the target to search for reachable tiles. |

[Kraken API]\
open fun [findApproximatePath](find-approximate-path.md)(start: WorldPoint, area: WorldArea): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;

Finds an approximate path to a random reachable tile within a specified WorldArea.

#### Return

A list of WorldPoints representing the path to a random point within the area.

#### Parameters

Kraken API

| | |
|---|---|
| start | The starting WorldPoint. |
| area | The WorldArea to search for reachable tiles within. |
