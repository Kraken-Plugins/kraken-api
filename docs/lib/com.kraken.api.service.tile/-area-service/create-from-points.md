//[lib](../../../index.md)/[com.kraken.api.service.tile](../index.md)/[AreaService](index.md)/[createFromPoints](create-from-points.md)

# createFromPoints

[Kraken API]\
open fun [createFromPoints](create-from-points.md)(points: [Collection](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/Collection.html)&lt;WorldPoint&gt;): [GameArea](../-game-area/index.md)

Creates an area from a raw collection of points. This must include all world points within a given &quot;outline&quot; of an area (i.e. the vertices making up a polygon). If you want to use an outline and generate the internal world points for an area use `createPolygonArea()` instead.

#### Return

GameArea the game area containing the WorldPoints specified in the set.

#### Parameters

Kraken API

| | |
|---|---|
| points | A set of WorldPoint objects used to create an area. |
