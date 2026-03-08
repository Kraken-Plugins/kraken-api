//[lib](../../../index.md)/[com.kraken.api.service.pathfinding](../index.md)/[GlobalPathfinder](index.md)/[findSparsePath](find-sparse-path.md)

# findSparsePath

[Kraken API]\
open fun [findSparsePath](find-sparse-path.md)(start: WorldPoint, target: WorldPoint): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;

Finds a sparse global path where waypoints are only kept when travel direction changes.

#### Return

A sparse path that always includes the start and final tile when a path exists.

#### Parameters

Kraken API

| | |
|---|---|
| start | The starting tile. |
| target | The destination tile. |
