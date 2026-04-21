//[kraken-api](../../../index.md)/[com.kraken.api.service.pathfinding](../index.md)/[GlobalPathfinder](index.md)/[findSparsePath](find-sparse-path.md)

# findSparsePath

[Kraken API]\
open fun [findSparsePath](find-sparse-path.md)(destination: WorldPoint): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;

Computes a sparse path consisting of waypoint clicks from the local player's current location to the specified destination using default configuration settings. 

This method collapses a dense path into fewer waypoint clicks while maintaining functionality and transport transitions. The primary purpose is to optimize movement paths by reducing unnecessary intermediate steps, making navigation more efficient for the player.

#### Return

A `List` of `WorldPoint` objects representing the sparse path to the destination. If the path could not be computed successfully, an empty `List` is returned.

#### Parameters

Kraken API

| | |
|---|---|
| destination | The target `WorldPoint` to which the sparse path should be generated. This represents the final goal location in the game world. |

[Kraken API]\
open fun [findSparsePath](find-sparse-path.md)(destination: WorldPoint, config: [GlobalPathfinderConfig](../-global-pathfinder-config/index.md)): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;

Computes a sparse path consisting of waypoint clicks from the local player's current location to the specified destination using the provided configuration. 

The computed path reduces unnecessary intermediate steps, collapsing a dense path into fewer waypoints while maintaining essential navigation functionality and transitions. The resulting path optimizes movement efficiency by prioritizing key waypoints over tile-by-tile precision.

#### Return

A `List` of `WorldPoint` objects representing the computed sparse path. If the path could not be computed successfully, an empty `List` is returned.

#### Parameters

Kraken API

| | |
|---|---|
| destination | The target `WorldPoint` to which the sparse path should be generated. Represents the final goal location in the game world. |
| config | An instance of `GlobalPathfinderConfig` containing configuration options. This influences pathfinding behaviors such as transport handling and constraints. |

[Kraken API]\
open fun [findSparsePath](find-sparse-path.md)(source: WorldPoint, destination: WorldPoint): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;

Finds a sparse path between the specified source and destination WorldPoints. 

 This method calculates a path consisting of fewer nodes to traverse, simplifying navigation or optimizing pathfinding where sparse routes are needed. 

#### Return

a List&lt;WorldPoint&gt; representing the sparse path from the source to the destination.

#### Parameters

Kraken API

| | |
|---|---|
| source | the starting WorldPoint of the path. |
| destination | the ending WorldPoint of the path. |

[Kraken API]\
open fun [findSparsePath](find-sparse-path.md)(source: WorldPoint, destination: WorldPoint, config: [GlobalPathfinderConfig](../-global-pathfinder-config/index.md)): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;

Finds a sparse path between the given source and destination points using the specified pathfinding configuration. 

 A sparse path consists of a subset of path points that represent key waypoints along the entire path. If the pathfinding result is incomplete, an empty list is returned.

#### Return

A List&lt;WorldPoint&gt; representing the sparse path if the pathfinding is complete; otherwise, an empty list if the pathfinding result is incomplete.

#### Parameters

Kraken API

| | |
|---|---|
| source | The starting point of the path. Must not be null. |
| destination | The ending point of the path. Must not be null. |
| config | The configuration settings to be used by the pathfinding algorithm. Must not be null. |
