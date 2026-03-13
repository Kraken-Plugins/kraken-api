//[kraken-api](../../../index.md)/[com.kraken.api.service.tile](../index.md)/[TileService](index.md)/[getReachableTilesFromTile](get-reachable-tiles-from-tile.md)

# getReachableTilesFromTile

[Kraken API]\
open fun [getReachableTilesFromTile](get-reachable-tiles-from-tile.md)(tile: WorldPoint, distance: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), ignoreCollision: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)): [HashMap](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/HashMap.html)&lt;WorldPoint, [Integer](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Integer.html)&gt;

This method calculates the distances to a specified tile in the game world using a breadth-first search (BFS) algorithm, considering movement restrictions and collision data. The distances are stored in a HashMap where the key is a WorldPoint (representing a tile location), and the value is the distance from the starting tile. The method accounts for movement flags that block movement in specific directions (east, west, north, south) and removes unreachable tiles based on collision data. 

 The method iterates over a range of distances, progressively updating reachable tiles and adding them to the tileDistances map. It checks if a tile can be reached by verifying its collision flags and whether it’s blocked for movement in any direction.

#### Return

A HashMap containing WorldPoints and their corresponding distances from the start tile.

#### Parameters

Kraken API

| | |
|---|---|
| tile | The starting tile for the distance calculation. |
| distance | The maximum distance to calculate to neighboring tiles. |
| ignoreCollision | If true, ignores collision data during the calculation. |
