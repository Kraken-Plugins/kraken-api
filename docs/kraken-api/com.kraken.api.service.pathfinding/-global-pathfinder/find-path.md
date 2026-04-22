//[kraken-api](../../../index.md)/[com.kraken.api.service.pathfinding](../index.md)/[GlobalPathfinder](index.md)/[findPath](find-path.md)

# findPath

[Kraken API]\
open fun [findPath](find-path.md)(destination: WorldPoint): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;

Finds a dense path from the local player to the target using default settings.

#### Return

A `List` of `WorldPoint` objects representing the computed path as a sequence of tiles. If the path could not be computed successfully, an empty list is returned.

#### Parameters

Kraken API

| | |
|---|---|
| destination | The destination point. |

[Kraken API]\
open fun [findPath](find-path.md)(destination: WorldPoint, config: [GlobalPathfinderConfig](../-global-pathfinder-config/index.md)): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;

Finds a dense tile-by-tile path from the local player's current location to the specified destination using the provided configuration.

#### Return

A `List` of `WorldPoint` objects representing the computed path as a sequence of tiles. If the path could not be computed successfully, an empty list is returned.

#### Parameters

Kraken API

| | |
|---|---|
| destination | The target `WorldPoint` to which the path should be generated. This represents the final goal location in the game world. |
| config | An instance of `GlobalPathfinderConfig` containing configuration options that influence path generation behaviors, including transport options or path constraints. |

[Kraken API]\
open fun [findPath](find-path.md)(source: WorldPoint, destination: WorldPoint): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;

Finds a dense tile-by-tile path between two `WorldPoint` locations using default configuration settings. 

This method calculates a detailed path from the specified source point to the destination point in the game world, utilizing the `DEFAULT_CONFIG` for pathfinding.

#### Return

A `List` of `WorldPoint` objects representing the computed path as a sequence of tiles. An empty list is returned if the path could not be computed successfully.

#### Parameters

Kraken API

| | |
|---|---|
| source | The starting `WorldPoint` for the path calculation. This represents the initial location in the game world. |
| destination | The target `WorldPoint` to which the path should be generated. This represents the final goal location in the game world. |

[Kraken API]\
open fun [findPath](find-path.md)(source: WorldPoint, destination: WorldPoint, config: [GlobalPathfinderConfig](../-global-pathfinder-config/index.md)): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;

Finds a path between two `WorldPoint` locations in the game world using the specified pathfinding configuration. 

This method calculates a tile-by-tile path from the specified source point to the destination point, leveraging the provided `GlobalPathfinderConfig` to determine the path generation behavior. If the computed path is valid and complete, it is returned as a `List` of `WorldPoint` objects. Otherwise, an empty list is returned.

#### Return

A `List` of `WorldPoint` objects representing the computed path. If the path could not be computed successfully, an empty `List` is returned.

#### Parameters

Kraken API

| | |
|---|---|
| source | The starting `WorldPoint` for the path calculation. Represents the initial location in the game world. |
| destination | The target `WorldPoint` to which the path should be generated. Represents the final desired location in the game world. |
| config | An instance of `GlobalPathfinderConfig` providing configuration options for the path generation, such as transport settings or path constraints. |
