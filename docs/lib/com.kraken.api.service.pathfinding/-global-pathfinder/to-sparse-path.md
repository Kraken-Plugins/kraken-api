//[lib](../../../index.md)/[com.kraken.api.service.pathfinding](../index.md)/[GlobalPathfinder](index.md)/[toSparsePath](to-sparse-path.md)

# toSparsePath

[Kraken API]\
open fun [toSparsePath](to-sparse-path.md)(densePath: [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;

Reduces a dense path into sparse waypoints by keeping tiles where direction changes.

#### Return

A sparse waypoint list.

#### Parameters

Kraken API

| | |
|---|---|
| densePath | A dense path including adjacent steps. |
