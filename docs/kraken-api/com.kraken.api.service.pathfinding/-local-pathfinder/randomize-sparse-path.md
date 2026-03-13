//[kraken-api](../../../index.md)/[com.kraken.api.service.pathfinding](../index.md)/[LocalPathfinder](index.md)/[randomizeSparsePath](randomize-sparse-path.md)

# randomizeSparsePath

[Kraken API]\
open fun [randomizeSparsePath](randomize-sparse-path.md)(start: WorldPoint, sparsePath: [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;, maxOffset: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), attemptsPerPoint: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), keepEndpoints: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;

Creates a randomized variation of a sparse path by slightly offsetting waypoints while ensuring each candidate waypoint is still reachable. 

The method samples up to `attemptsPerPoint` random offsets per waypoint within `maxOffset` tiles. A candidate is accepted only if it is present in the set of reachable tiles from the selected origin (the start for the first waypoint when provided, otherwise the original waypoint). If no candidate is valid, the original waypoint is kept.

#### Return

A new list of WorldPoints representing the randomized sparse path.

#### Parameters

Kraken API

| | |
|---|---|
| start | The starting point for the path. Used as the reachability origin for the first waypoint when provided. |
| sparsePath | The sparse path to randomize. |
| maxOffset | The maximum offset (in tiles) to apply to a waypoint in any direction. |
| attemptsPerPoint | The number of random candidates to try per waypoint. |
| keepEndpoints | Whether to keep the first and last waypoint unchanged. |

[Kraken API]\
open fun [randomizeSparsePath](randomize-sparse-path.md)(start: WorldPoint, sparsePath: [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;, maxOffset: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;

Convenience overload that keeps endpoints and uses a default attempt count.

#### Return

A new list of WorldPoints representing the randomized sparse path.

#### Parameters

Kraken API

| | |
|---|---|
| start | The starting point for the path. |
| sparsePath | The sparse path to randomize. |
| maxOffset | The maximum offset (in tiles) to apply to a waypoint in any direction. |
