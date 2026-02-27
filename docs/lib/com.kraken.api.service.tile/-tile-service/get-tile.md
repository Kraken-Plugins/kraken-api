//[lib](../../../index.md)/[com.kraken.api.service.tile](../index.md)/[TileService](index.md)/[getTile](get-tile.md)

# getTile

[Kraken API]\
open fun [getTile](get-tile.md)(x: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), y: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): Tile

This method retrieves the tile at the specified coordinates (x, y) on the current plane. It first creates a WorldPoint for the given coordinates and checks if the point is within the scene using the `isInScene` method. If the WorldPoint is valid and within the scene, it converts the WorldPoint to a LocalPoint, then retrieves and returns the corresponding Tile from the game scene. 

 If the WorldPoint is out of bounds or the LocalPoint is null, the method returns null to indicate that no valid tile is found at the given coordinates.

#### Return

The Tile at the specified coordinates, or null if the tile is invalid or not in the scene.

#### Parameters

Kraken API

| | |
|---|---|
| x | The x-coordinate of the tile. |
| y | The y-coordinate of the tile. |

[Kraken API]\
open fun [getTile](get-tile.md)(point: WorldPoint): Tile

Returns the Tile for a given WorldPoint.

#### Return

The tile for a given WorldPoint.

#### Parameters

Kraken API

| | |
|---|---|
| point | WorldPoint to get the tile for |
