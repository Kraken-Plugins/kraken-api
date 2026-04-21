//[kraken-api](../../../index.md)/[com.kraken.api.service.pathfinding](../index.md)/[GlobalPathfinder](index.md)/[findPathResult](find-path-result.md)

# findPathResult

[Kraken API]\
open fun [findPathResult](find-path-result.md)(destination: WorldPoint): [GlobalPathfinder.PathResult](-path-result/index.md)

Finds the path result from the local player's current position to the specified destination using the default configuration. 

This method resolves the local player's current position and calculates the path to the given destination point in the world.

#### Return

a @PathResult representing the calculated path from the local player's position to the specified destination

#### Parameters

Kraken API

| | |
|---|---|
| destination | the target @WorldPoint to which the path should be calculated |

[Kraken API]\
open fun [findPathResult](find-path-result.md)(destination: WorldPoint, config: [GlobalPathfinderConfig](../-global-pathfinder-config/index.md)): [GlobalPathfinder.PathResult](-path-result/index.md)

Finds and returns the [PathResult](-path-result/index.md) for navigating to the specified destination using the given pathfinder configuration. 

This method computes the path starting from the local player's position and determines the best route to the specified destination based on the provided configuration parameters.

#### Return

a [PathResult](-path-result/index.md) object containing the computed path data, including waypoints, traversal status, and relevant metadata.

#### Parameters

Kraken API

| | |
|---|---|
| destination | the target WorldPoint to which the path should be calculated. It represents the endpoint of the pathfinding operation. |
| config | the [GlobalPathfinderConfig](../-global-pathfinder-config/index.md) containing configuration parameters such as traversal rules, movement constraints, and additional pathfinding options. |

[Kraken API]\
open fun [findPathResult](find-path-result.md)(source: WorldPoint, destination: WorldPoint): [GlobalPathfinder.PathResult](-path-result/index.md)

Finds the path result between a source point and a destination point in the world. 

 This method calculates the path using the provided source and destination points within the given world configuration.

#### Return

A `PathResult` object representing the computed path between the source and destination points, including any metadata about the pathfinding process.

#### Parameters

Kraken API

| | |
|---|---|
| source | The starting point in the world. Must not be null. |
| destination | The end point in the world to navigate to. Must not be null. |

[Kraken API]\
open fun [findPathResult](find-path-result.md)(source: WorldPoint, destination: WorldPoint, config: [GlobalPathfinderConfig](../-global-pathfinder-config/index.md)): [GlobalPathfinder.PathResult](-path-result/index.md)

Finds the path result based on the given source and destination points within a global pathfinding context. This method performs the pathfinding operation and returns a `PathResult` object containing the path details. If the provided source or destination is `null`, an empty `PathResult` is returned. 

**Thread-safety:** This method is synchronized to ensure thread safety during execution.

#### Return

A `PathResult` object containing the pathfinding results. If the operation fails or there are no valid targets, an empty `PathResult` is returned. The returned result includes details such as the computed path, resolved configuration, and other related metadata.

#### Parameters

Kraken API

| | |
|---|---|
| source | The starting point of the pathfinding operation. Must be a valid `WorldPoint`. If `null`, an empty `PathResult` is returned. |
| destination | The endpoint of the pathfinding operation. Must be a valid `WorldPoint`. If `null`, an empty `PathResult` is returned. |
| config | Optional configuration object (`GlobalPathfinderConfig`) for the pathfinding process. If `null`, the default configuration is used. |
