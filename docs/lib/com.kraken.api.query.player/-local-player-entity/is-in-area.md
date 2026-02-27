//[lib](../../../index.md)/[com.kraken.api.query.player](../index.md)/[LocalPlayerEntity](index.md)/[isInArea](is-in-area.md)

# isInArea

[Kraken API]\
open fun [isInArea](is-in-area.md)(worldPoint: WorldPoint): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Checks if the player is within 5 tiles of a given WorldPoint.

#### Return

`true` if the player is within the specified distance, `false` otherwise.

#### Parameters

Kraken API

| | |
|---|---|
| worldPoint | The WorldPoint to check proximity to. |

[Kraken API]\
open fun [isInArea](is-in-area.md)(area: [GameArea](../../com.kraken.api.service.tile/-game-area/index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Checks if the player is within the game area.

#### Return

True if the player is within the game area and false otherwise.

#### Parameters

Kraken API

| | |
|---|---|
| area | The [GameArea](../../com.kraken.api.service.tile/-game-area/index.md) to check. |

[Kraken API]\
open fun [isInArea](is-in-area.md)(worldPoint: WorldPoint, radius: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Checks if the player is within a specified distance of a given WorldPoint.

#### Return

`true` if the player is within the specified distance, `false` otherwise.

#### Parameters

Kraken API

| | |
|---|---|
| worldPoint | The WorldPoint to check proximity to. |
| radius | The radius (in tiles) around the `worldPoint` to check. |

[Kraken API]\
open fun [isInArea](is-in-area.md)(worldPoint: WorldPoint, xRadius: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), yRadius: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Checks if the player is within a specified area around a given WorldPoint.

#### Return

`true` if the player is within the specified area, `false` otherwise.

#### Parameters

Kraken API

| | |
|---|---|
| worldPoint | The WorldPoint to check proximity to. |
| xRadius | The horizontal radius (in tiles) around the `worldPoint`. |
| yRadius | The vertical radius (in tiles) around the `worldPoint`. |
