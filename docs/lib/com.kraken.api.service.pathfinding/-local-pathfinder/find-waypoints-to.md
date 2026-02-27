//[lib](../../../index.md)/[com.kraken.api.service.pathfinding](../index.md)/[LocalPathfinder](index.md)/[findWaypointsTo](find-waypoints-to.md)

# findWaypointsTo

[Kraken API]\
open fun [findWaypointsTo](find-waypoints-to.md)(from: Tile, to: Tile): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;Tile&gt;

Finds the waypoints needed to navigate from the starting `Tile` to the destination `Tile`. This method calculates a path using directional and distance matrices, while considering collision data within the game world. If a direct path is not possible, it searches for the closest accessible tile around the destination. 

Note that both the starting and destination tiles must reside on the same plane (z-coordinate). If they are not, this method will return `null`.

 Credit to Vitalite and TonicBox for this methods implementation. It was taken from their scene API: [Link](https://github.com/Tonic-Box/VitaLite/blob/main/api/src/main/java/com/tonic/api/game/SceneAPI.java)

#### Return

a `List` of `Tile` objects representing the calculated waypoints to the destination, or `null` if the path cannot be calculated (e.g., due to inaccessible areas or mismatched planes).

#### Parameters

Kraken API

| | |
|---|---|
| from | the starting `Tile` from which the path needs to be calculated. |
| to | the destination `Tile` to which the path needs to lead. |
