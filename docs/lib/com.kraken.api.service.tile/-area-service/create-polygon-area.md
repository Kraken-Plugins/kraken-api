//[lib](../../../index.md)/[com.kraken.api.service.tile](../index.md)/[AreaService](index.md)/[createPolygonArea](create-polygon-area.md)

# createPolygonArea

[Kraken API]\
open fun [createPolygonArea](create-polygon-area.md)(vertices: [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;): [GameArea](../-game-area/index.md)

Creates a complex shape from a list of vertices. This rasterizes the polygon: it finds all discrete tiles inside the shape.

#### Return

GameArea the game area containing the WorldPoints within the specified vertices

#### Parameters

Kraken API

| | |
|---|---|
| vertices | A list of vertices |

[Kraken API]\
open fun [createPolygonArea](create-polygon-area.md)(vertices: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;WorldPoint&gt;): [GameArea](../-game-area/index.md)

Creates a complex shape (e.g., L-shape) from vertices. This rasterizes the polygon: it finds all discrete tiles inside the shape.

#### Return

GameArea the game area containing the WorldPoints within the specified vertices

#### Parameters

Kraken API

| | |
|---|---|
| vertices | A set of vertices making up the bounds of the polygon area |
