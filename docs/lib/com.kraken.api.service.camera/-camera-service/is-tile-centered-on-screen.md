//[lib](../../../index.md)/[com.kraken.api.service.camera](../index.md)/[CameraService](index.md)/[isTileCenteredOnScreen](is-tile-centered-on-screen.md)

# isTileCenteredOnScreen

[Kraken API]\
open fun [isTileCenteredOnScreen](is-tile-centered-on-screen.md)(tile: LocalPoint, marginPercentage: [Double](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-double/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Determines whether the specified tile is centered on the screen within a given tolerance. 

 Projects the tile to screen space, computes its bounding rectangle, and then checks whether that rectangle lies entirely inside a centered &quot;box&quot; whose width and height are the given percentage of the viewport dimensions. 

#### Return

`true` if the tile's screen bounds lie entirely within the centered margin box; `false` if the tile cannot be projected or lies outside that box

#### Parameters

Kraken API

| | |
|---|---|
| tile | the local tile coordinate to test (may not be null) |
| marginPercentage | the size of the centered tolerance box, expressed as a percentage of the viewport (e.g. 10.0 for 10%) |

[Kraken API]\
open fun [isTileCenteredOnScreen](is-tile-centered-on-screen.md)(tile: LocalPoint): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Determines whether the specified tile is centered on the screen, using a default margin tolerance of 10%.

#### Return

`true` if the tile's screen bounds lie entirely within the centered 10% margin box; `false` otherwise

#### Parameters

Kraken API

| | |
|---|---|
| tile | the local tile coordinate to test (may not be null) |

#### See also

| |
|---|
| [isTileCenteredOnScreen(LocalPoint, double)](is-tile-centered-on-screen.md) |
