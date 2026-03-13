//[kraken-api](../../../index.md)/[com.kraken.api.service.camera](../index.md)/[CameraService](index.md)/[isTileOnScreen](is-tile-on-screen.md)

# isTileOnScreen

[Kraken API]\
open fun [isTileOnScreen](is-tile-on-screen.md)(tileObject: TileObject): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Checks if the specified tile object is visible on the screen.

#### Return

true if the tile object is within the viewport bounds, false otherwise

#### Parameters

Kraken API

| | |
|---|---|
| tileObject | the tile object to check |

[Kraken API]\
open fun [isTileOnScreen](is-tile-on-screen.md)(localPoint: LocalPoint): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Checks if the specified local point is visible on the screen. Verifies that the tile polygon intersects with the viewport and that the tile is in front of the camera.

#### Return

true if the tile is within the viewport bounds and in front of the camera, false otherwise

#### Parameters

Kraken API

| | |
|---|---|
| localPoint | the local point to check |
