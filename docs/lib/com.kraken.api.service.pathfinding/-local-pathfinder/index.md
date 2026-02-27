//[lib](../../../index.md)/[com.kraken.api.service.pathfinding](../index.md)/[LocalPathfinder](index.md)

# LocalPathfinder

[Kraken API]\
open class [LocalPathfinder](index.md)

 The `LocalPathfinder` class is responsible for pathfinding within a local 104x104 tile 3D game scene. It provides methods to compute paths using Breadth First Search (BFS), determine sparse paths for waypoints where directional changes occur, render paths visually, and validate the reachability of points within the currently loaded scene. This class is useful for AI, navigation, and player movement scenarios. 

 The class supports the following functionalities: 

- Compute the shortest path between points in a game scene, including computing sparse waypoints with [findSparsePath](find-sparse-path.md).
- Render computed paths on a graphical interface using methods like [renderMinimapPath](render-minimap-path.md) and [renderPath](render-path.md).
- Handle approximate pathfinding if exact target points are not reachable.

## Constructors

| | |
|---|---|
| [LocalPathfinder](-local-pathfinder.md) | [Kraken API]<br>constructor() |

## Functions

| Name | Summary |
|---|---|
| [findApproximatePath](find-approximate-path.md) | [Kraken API]<br>open fun [findApproximatePath](find-approximate-path.md)(start: WorldPoint, area: WorldArea): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;<br>Finds an approximate path to a random reachable tile within a specified WorldArea.<br>[Kraken API]<br>open fun [findApproximatePath](find-approximate-path.md)(start: WorldPoint, target: WorldPoint): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;<br>Finds an approximate path to a random reachable tile within a default radius of 5 tiles around the target location.<br>[Kraken API]<br>open fun [findApproximatePath](find-approximate-path.md)(start: WorldPoint, target: WorldPoint, radius: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;<br>Finds an approximate path to a random reachable tile within a specified radius around the target location. |
| [findApproximatePathWithBackoff](find-approximate-path-with-backoff.md) | [Kraken API]<br>open fun [findApproximatePathWithBackoff](find-approximate-path-with-backoff.md)(start: WorldPoint, target: WorldPoint, radius: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;<br>Attempts to find a path to a reachable tile within a specified radius of the target. |
| [findEdgeOfScene](find-edge-of-scene.md) | [Kraken API]<br>open fun [findEdgeOfScene](find-edge-of-scene.md)(target: WorldPoint): WorldPoint |
| [findPath](find-path.md) | [Kraken API]<br>open fun [findPath](find-path.md)(start: WorldPoint, target: WorldPoint): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;<br>Calculates and returns a path from a starting point to a target point within the game world. |
| [findPathWithBackoff](find-path-with-backoff.md) | [Kraken API]<br>open fun [findPathWithBackoff](find-path-with-backoff.md)(start: WorldPoint, target: WorldPoint): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;<br>Attempts to find a path to the target. |
| [findSparsePath](find-sparse-path.md) | [Kraken API]<br>open fun [findSparsePath](find-sparse-path.md)(start: WorldPoint, target: WorldPoint): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;<br>Finds a sparse path between a starting point and a target point by filtering out unnecessary intermediate points from a previously computed dense path. |
| [findWaypointsTo](find-waypoints-to.md) | [Kraken API]<br>open fun [findWaypointsTo](find-waypoints-to.md)(from: Tile, to: Tile): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;Tile&gt;<br>Finds the waypoints needed to navigate from the starting `Tile` to the destination `Tile`. |
| [randomizeSparsePath](randomize-sparse-path.md) | [Kraken API]<br>open fun [randomizeSparsePath](randomize-sparse-path.md)(start: WorldPoint, sparsePath: [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;, maxOffset: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;<br>Convenience overload that keeps endpoints and uses a default attempt count.<br>[Kraken API]<br>open fun [randomizeSparsePath](randomize-sparse-path.md)(start: WorldPoint, sparsePath: [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;, maxOffset: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), attemptsPerPoint: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), keepEndpoints: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;<br>Creates a randomized variation of a sparse path by slightly offsetting waypoints while ensuring each candidate waypoint is still reachable. |
| [reachableTiles](reachable-tiles.md) | [Kraken API]<br>open fun [reachableTiles](reachable-tiles.md)(origin: WorldPoint): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;<br>Returns a list of all reachable tiles from the origins position using a breadth-first search algorithm. |
| [renderMinimapPath](render-minimap-path.md) | [Kraken API]<br>open fun [renderMinimapPath](render-minimap-path.md)(path: [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;, graphics: [Graphics2D](https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/java/awt/Graphics2D.html), color: [Color](https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/java/awt/Color.html))<br>Renders a path on the minimap. |
| [renderPath](render-path.md) | [Kraken API]<br>open fun [renderPath](render-path.md)(path: [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;, graphics: [Graphics2D](https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/java/awt/Graphics2D.html), pathColor: [Color](https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/java/awt/Color.html))<br>Renders a series of tiles representing a path on the game canvas. |
