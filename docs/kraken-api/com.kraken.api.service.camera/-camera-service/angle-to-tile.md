//[kraken-api](../../../index.md)/[com.kraken.api.service.camera](../index.md)/[CameraService](index.md)/[angleToTile](angle-to-tile.md)

# angleToTile

[Kraken API]\
open fun [angleToTile](angle-to-tile.md)(t: Actor): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)

Calculates the angle in degrees from the local player to the specified actor.

#### Return

the angle in degrees (0-359), where 0 is east and increases counter-clockwise

#### Parameters

Kraken API

| | |
|---|---|
| t | the target actor |

[Kraken API]\
open fun [angleToTile](angle-to-tile.md)(t: TileObject): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)

Calculates the angle in degrees from the local player to the specified tile object.

#### Return

the angle in degrees (0-359), where 0 is east and increases counter-clockwise

#### Parameters

Kraken API

| | |
|---|---|
| t | the target tile object |

[Kraken API]\
open fun [angleToTile](angle-to-tile.md)(localPoint: LocalPoint): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)

Calculates the angle in degrees from the local player to the specified local point.

#### Return

the angle in degrees (0-359), where 0 is east and increases counter-clockwise

#### Parameters

Kraken API

| | |
|---|---|
| localPoint | the target local point |

[Kraken API]\
open fun [angleToTile](angle-to-tile.md)(worldPoint: WorldPoint): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)

Calculates the angle in degrees from the local player to the specified world point.

#### Return

the angle in degrees (0-359), where 0 is east and increases counter-clockwise

#### Parameters

Kraken API

| | |
|---|---|
| worldPoint | the target world point |
