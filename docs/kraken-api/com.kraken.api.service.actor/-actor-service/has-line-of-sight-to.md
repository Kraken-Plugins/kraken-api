//[kraken-api](../../../index.md)/[com.kraken.api.service.actor](../index.md)/[ActorService](index.md)/[hasLineOfSightTo](has-line-of-sight-to.md)

# hasLineOfSightTo

[Kraken API]\
open fun [hasLineOfSightTo](has-line-of-sight-to.md)(source: WorldPoint, other: WorldPoint): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Checks if there is a clear line of sight between two world points.

#### Return

True if there is an unobstructed line of sight, false otherwise.

#### Parameters

Kraken API

| | |
|---|---|
| source | The starting WorldPoint. |
| other | The target WorldPoint. |

[Kraken API]\
open fun [hasLineOfSightTo](has-line-of-sight-to.md)(source: Tile, other: Tile): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Checks if there is a clear line of sight between two scene tiles. Automatically dispatches to the client thread to safely access collision maps.

#### Return

True if there is an unobstructed line of sight, false otherwise.

#### Parameters

Kraken API

| | |
|---|---|
| source | The starting Tile. |
| other | The target Tile. |
