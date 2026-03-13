//[kraken-api](../../../index.md)/[com.kraken.api.service.pathfinding](../index.md)/[LocalPathfinder](index.md)/[reachableTiles](reachable-tiles.md)

# reachableTiles

[Kraken API]\
open fun [reachableTiles](reachable-tiles.md)(origin: WorldPoint): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;

Returns a list of all reachable tiles from the origins position using a breadth-first search algorithm. This method considers the collision data to determine which tiles can be reached.

#### Return

A list of WorldPoint objects representing all reachable tiles from the origin.

#### Parameters

Kraken API

| | |
|---|---|
| origin | The point to query from |
