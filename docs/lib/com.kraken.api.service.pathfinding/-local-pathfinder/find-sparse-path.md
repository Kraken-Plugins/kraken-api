//[lib](../../../index.md)/[com.kraken.api.service.pathfinding](../index.md)/[LocalPathfinder](index.md)/[findSparsePath](find-sparse-path.md)

# findSparsePath

[Kraken API]\
open fun [findSparsePath](find-sparse-path.md)(start: WorldPoint, target: WorldPoint): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;

Finds a sparse path between a starting point and a target point by filtering out unnecessary intermediate points from a previously computed dense path. 

The method calculates directional changes in the dense path and retains only the waypoints where the direction changes, along with the final destination. This ensures a simplified path that accurately represents the required turns or path changes while omitting redundant points.

#### Return

A @List of @WorldPoint objects representing the sparse path. Returns an empty list if no path can be computed.

#### Parameters

Kraken API

| | |
|---|---|
| start | @WorldPoint representing the starting location of the path. |
| target | @WorldPoint representing the destination point of the path. |
