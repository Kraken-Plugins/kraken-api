//[kraken-api](../../../index.md)/[com.kraken.api.service.pathfinding](../index.md)/[LocalPathfinder](index.md)/[findPathWithBackoff](find-path-with-backoff.md)

# findPathWithBackoff

[Kraken API]\
open fun [findPathWithBackoff](find-path-with-backoff.md)(start: WorldPoint, target: WorldPoint): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;

Attempts to find a path to the target. If the target is unreachable, it attempts to find a path to a tile closer to the start point by &quot;backing off&quot; from the target in an exponential/incremental fashion. 

The backoff strategy works by calculating points along the line between the target and the start. It steps back by 1 tile, then 3, then 6, then 10, etc., until a reachable path is found or the search backs up all the way to the start.

#### Return

A List of WorldPoints representing the path to the target or the best approximate location found. Returns an empty list if no path can be found.

#### Parameters

Kraken API

| | |
|---|---|
| start | The starting WorldPoint. |
| target | The desired target WorldPoint. |
