//[kraken-api](../../../index.md)/[com.kraken.api.service.camera](../index.md)/[CameraService](index.md)/[centerTileOnScreen](center-tile-on-screen.md)

# centerTileOnScreen

[Kraken API]\
open fun [centerTileOnScreen](center-tile-on-screen.md)(tile: LocalPoint, marginPercentage: [Double](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-double/index.html))

Rotates the camera to center on the specified tile, if it is not already within the given margin tolerance. 

 Computes the bearing from the camera to the tile, adjusts it into a [0–360) range, and then issues a small-angle camera turn if [isTileCenteredOnScreen](is-tile-centered-on-screen.md) returns `false`. 

#### Parameters

Kraken API

| | |
|---|---|
| tile | the local tile coordinate to center on (may not be null) |
| marginPercentage | the size of the centered tolerance box, expressed as a percentage of the viewport (e.g. 10.0 for 10%) |

#### See also

| |
|---|
| [angleToTile(LocalPoint)](angle-to-tile.md) |
| [setAngle(int, int)](set-angle.md) |

[Kraken API]\
open fun [centerTileOnScreen](center-tile-on-screen.md)(tile: LocalPoint)

Rotates the camera to center on the specified tile, using a default margin tolerance of 10%.

#### Parameters

Kraken API

| | |
|---|---|
| tile | the local tile coordinate to center on (may not be null) |

#### See also

| |
|---|
| [centerTileOnScreen(LocalPoint, double)](center-tile-on-screen.md) |
