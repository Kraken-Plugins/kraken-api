//[kraken-api](../../../index.md)/[com.kraken.api.service.pathfinding](../index.md)/[GlobalPathfinder](index.md)

# GlobalPathfinder

[Kraken API]\
open class [GlobalPathfinder](index.md)

## Constructors

| | |
|---|---|
| [GlobalPathfinder](-global-pathfinder.md) | [Kraken API]<br>constructor() |

## Types

| Name | Summary |
|---|---|
| [PathResult](-path-result/index.md) | [Kraken API]<br>class [PathResult](-path-result/index.md) |
| [TransportUsage](-transport-usage/index.md) | [Kraken API]<br>class [TransportUsage](-transport-usage/index.md)<br>Describes a single transport edge chosen in the final route. |

## Functions

| Name | Summary |
|---|---|
| [clearLastResult](clear-last-result.md) | [Kraken API]<br>open fun [clearLastResult](clear-last-result.md)()<br>Clears the cached route used by overlays and callers inspecting the last result. |
| [findPath](find-path.md) | [Kraken API]<br>open fun [findPath](find-path.md)(destination: WorldPoint): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;<br>Finds a dense path from the local player to the target using default settings.<br>[Kraken API]<br>open fun [findPath](find-path.md)(source: WorldPoint, destination: WorldPoint): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;<br>Finds a dense tile-by-tile path between two `WorldPoint` locations using default configuration settings.<br>[Kraken API]<br>open fun [findPath](find-path.md)(destination: WorldPoint, config: [GlobalPathfinderConfig](../-global-pathfinder-config/index.md)): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;<br>Finds a dense tile-by-tile path from the local player's current location to the specified destination using the provided configuration.<br>[Kraken API]<br>open fun [findPath](find-path.md)(source: WorldPoint, destination: WorldPoint, config: [GlobalPathfinderConfig](../-global-pathfinder-config/index.md)): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;<br>Finds a path between two `WorldPoint` locations in the game world using the specified pathfinding configuration. |
| [findPathResult](find-path-result.md) | [Kraken API]<br>open fun [findPathResult](find-path-result.md)(destination: WorldPoint): [GlobalPathfinder.PathResult](-path-result/index.md)<br>Finds the path result from the local player's current position to the specified destination using the default configuration.<br>[Kraken API]<br>open fun [findPathResult](find-path-result.md)(source: WorldPoint, destination: WorldPoint): [GlobalPathfinder.PathResult](-path-result/index.md)<br>Finds the path result between a source point and a destination point in the world.<br>[Kraken API]<br>open fun [findPathResult](find-path-result.md)(destination: WorldPoint, config: [GlobalPathfinderConfig](../-global-pathfinder-config/index.md)): [GlobalPathfinder.PathResult](-path-result/index.md)<br>Finds and returns the [PathResult](-path-result/index.md) for navigating to the specified destination using the given pathfinder configuration.<br>[Kraken API]<br>open fun [findPathResult](find-path-result.md)(source: WorldPoint, destination: WorldPoint, config: [GlobalPathfinderConfig](../-global-pathfinder-config/index.md)): [GlobalPathfinder.PathResult](-path-result/index.md)<br>Finds the path result based on the given source and destination points within a global pathfinding context. |
| [findSparsePath](find-sparse-path.md) | [Kraken API]<br>open fun [findSparsePath](find-sparse-path.md)(destination: WorldPoint): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;<br>Computes a sparse path consisting of waypoint clicks from the local player's current location to the specified destination using default configuration settings.<br>[Kraken API]<br>open fun [findSparsePath](find-sparse-path.md)(source: WorldPoint, destination: WorldPoint): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;<br>Finds a sparse path between the specified source and destination WorldPoints.<br>[Kraken API]<br>open fun [findSparsePath](find-sparse-path.md)(destination: WorldPoint, config: [GlobalPathfinderConfig](../-global-pathfinder-config/index.md)): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;<br>Computes a sparse path consisting of waypoint clicks from the local player's current location to the specified destination using the provided configuration.<br>[Kraken API]<br>open fun [findSparsePath](find-sparse-path.md)(source: WorldPoint, destination: WorldPoint, config: [GlobalPathfinderConfig](../-global-pathfinder-config/index.md)): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;<br>Finds a sparse path between the given source and destination points using the specified pathfinding configuration. |
