//[kraken-api](../../../index.md)/[com.kraken.api.service.tile](../index.md)/[TileService](index.md)

# TileService

open class [TileService](index.md)

Returns a normal WorldPoint given a world point that originated in an instance.

#### Return

a normalized WorldPoint from an instance WorldPoint

#### Parameters

Kraken API

| | |
|---|---|
| worldPoint | WorldPoint to convert |

## Constructors

| | |
|---|---|
| [TileService](-tile-service.md) | [Kraken API]<br>constructor() |

## Functions

| Name | Summary |
|---|---|
| [fromInstance](from-instance.md) | [Kraken API]<br>open fun [fromInstance](from-instance.md)(worldPoint: WorldPoint): WorldPoint<br>Gets the coordinate of the tile that contains the passed world point, accounting for instances. |
| [fromWorldInstance](from-world-instance.md) | [Kraken API]<br>open fun [fromWorldInstance](from-world-instance.md)(worldPoint: WorldPoint): LocalPoint<br>Used to convert a WorldPoint in an instance to a LocalPoint |
| [getObjectComposition](get-object-composition.md) | [Kraken API]<br>open fun [getObjectComposition](get-object-composition.md)(tileObject: TileObject): ObjectComposition<br>Returns the object composition for a given TileObject. |
| [getReachableTilesFromTile](get-reachable-tiles-from-tile.md) | [Kraken API]<br>open fun [getReachableTilesFromTile](get-reachable-tiles-from-tile.md)(tile: WorldPoint, distance: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), ignoreCollision: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)): [HashMap](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/HashMap.html)&lt;WorldPoint, [Integer](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Integer.html)&gt;<br>This method calculates the distances to a specified tile in the game world using a breadth-first search (BFS) algorithm, considering movement restrictions and collision data. |
| [getTile](get-tile.md) | [Kraken API]<br>open fun [getTile](get-tile.md)(point: WorldPoint): Tile<br>Returns the Tile for a given WorldPoint.<br>[Kraken API]<br>open fun [getTile](get-tile.md)(x: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), y: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): Tile<br>This method retrieves the tile at the specified coordinates (x, y) on the current plane. |
| [isObjectReachable](is-object-reachable.md) | [Kraken API]<br>open fun [isObjectReachable](is-object-reachable.md)(obj: GameObject): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Checks if a GameObject is reachable. |
| [isTileReachable](is-tile-reachable.md) | [Kraken API]<br>open fun [isTileReachable](is-tile-reachable.md)(targetPoint: WorldPoint): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>This method checks if a given target tile (WorldPoint) is reachable from the player's current location, considering collision data and the plane of the world. |
| [localToWorldDistance](local-to-world-distance.md) | [Kraken API]<br>open fun [localToWorldDistance](local-to-world-distance.md)(distance: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Integer](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Integer.html)<br>Returns the distance from a local point to another local point in world point distance. |
| [toInstance](to-instance.md) | [Kraken API]<br>open fun [toInstance](to-instance.md)(worldPoint: WorldPoint): [ArrayList](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/ArrayList.html)&lt;WorldPoint&gt;<br>Converts a world point into a list of instanced world points |
| [worldToLocalDistance](world-to-local-distance.md) | [Kraken API]<br>open fun [worldToLocalDistance](world-to-local-distance.md)(distance: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Integer](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Integer.html)<br>Returns the distance from a world point to another world point in local point distance. |
