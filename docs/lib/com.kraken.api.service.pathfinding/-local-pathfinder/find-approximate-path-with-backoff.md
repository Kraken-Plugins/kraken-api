//[lib](../../../index.md)/[com.kraken.api.service.pathfinding](../index.md)/[LocalPathfinder](index.md)/[findApproximatePathWithBackoff](find-approximate-path-with-backoff.md)

# findApproximatePathWithBackoff

[Kraken API]\
open fun [findApproximatePathWithBackoff](find-approximate-path-with-backoff.md)(start: WorldPoint, target: WorldPoint, radius: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;

Attempts to find a path to a reachable tile within a specified radius of the target. If no reachable tiles are found near the target, it linearly &quot;backs off&quot; from the target towards the start point and searches for reachable tiles near those intermediate points. 

This strategy is useful for getting as close as possible to a destination that might be completely unreachable (e.g., inside a wall or on an island), by finding the closest valid cluster of tiles along the path.

#### Return

A list of WorldPoints representing the path to the best found location, or an empty list if none found.

#### Parameters

Kraken API

| | |
|---|---|
| start | The starting WorldPoint. |
| target | The target WorldPoint. |
| radius | The radius (Chebyshev distance) to search for reachable tiles around the target/backoff points. |
