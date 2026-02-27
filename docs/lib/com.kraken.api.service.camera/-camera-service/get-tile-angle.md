//[lib](../../../index.md)/[com.kraken.api.service.camera](../index.md)/[CameraService](index.md)/[getTileAngle](get-tile-angle.md)

# getTileAngle

[Kraken API]\
open fun [getTileAngle](get-tile-angle.md)(actor: Actor): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)

Calculates the camera angle needed to face the specified actor. Adjusts the mathematical angle to camera coordinates.

#### Return

the camera angle in degrees (0-359)

#### Parameters

Kraken API

| | |
|---|---|
| actor | the target actor |

[Kraken API]\
open fun [getTileAngle](get-tile-angle.md)(tileObject: TileObject): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)

Calculates the camera angle needed to face the specified tile object. Adjusts the mathematical angle to camera coordinates.

#### Return

the camera angle in degrees (0-359)

#### Parameters

Kraken API

| | |
|---|---|
| tileObject | the target tile object |
