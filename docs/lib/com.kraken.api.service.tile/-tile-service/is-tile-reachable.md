//[lib](../../../index.md)/[com.kraken.api.service.tile](../index.md)/[TileService](index.md)/[isTileReachable](is-tile-reachable.md)

# isTileReachable

[Kraken API]\
open fun [isTileReachable](is-tile-reachable.md)(targetPoint: WorldPoint): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

This method checks if a given target tile (WorldPoint) is reachable from the player's current location, considering collision data and the plane of the world. The method uses a breadth-first search (BFS) algorithm to traverse neighboring tiles while checking for movement blocks in the four cardinal directions (north, south, east, west). It ensures the target tile is within the same plane as the player and that movement between tiles is not blocked. 

 The method initializes a queue to explore the world grid, marking visited tiles to avoid revisiting. It checks the flags for collision data to determine whether movement is allowed in each direction, and only adds neighboring tiles to the queue if they are not blocked. Finally, it verifies if the target point has been visited during the traversal and returns true if reachable, false otherwise.

#### Return

True if the target tile is reachable from the player's location, otherwise false.

#### Parameters

Kraken API

| | |
|---|---|
| targetPoint | The WorldPoint representing the target tile to check for reachability. |
