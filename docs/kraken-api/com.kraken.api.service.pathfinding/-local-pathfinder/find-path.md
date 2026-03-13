//[kraken-api](../../../index.md)/[com.kraken.api.service.pathfinding](../index.md)/[LocalPathfinder](index.md)/[findPath](find-path.md)

# findPath

[Kraken API]\
open fun [findPath](find-path.md)(start: WorldPoint, target: WorldPoint): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;

Calculates and returns a path from a starting point to a target point within the game world. If the target point is outside the scene, the method attempts to determine the edge of the scene closest to the target and calculates a path to that edge instead. 

If the target point is within the loaded scene, the method directly computes the path to the target using the `findScenePath` method. If the target is outside the scene, it finds the nearest edge point to the target and calculates a path to that point.

#### Return

A @List of @WorldPoint objects representing the calculated path from the start to the target (or closest reachable edge point). If no path can be calculated, an empty list is returned.

#### Parameters

Kraken API

| | |
|---|---|
| start | @WorldPoint representing the starting location of the path. |
| target | @WorldPoint representing the destination point of the path. |
