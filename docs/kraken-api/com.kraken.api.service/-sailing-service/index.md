//[kraken-api](../../../index.md)/[com.kraken.api.service](../index.md)/[SailingService](index.md)

# SailingService

[Kraken API]\
open class [SailingService](index.md)

## Constructors

| | |
|---|---|
| [SailingService](-sailing-service.md) | [Kraken API]<br>constructor() |

## Types

| Name | Summary |
|---|---|
| [Direction](-direction/index.md) | [Kraken API]<br>enum [Direction](-direction/index.md) |

## Properties

| Name | Summary |
|---|---|
| [NAVIGATING_VARBIT](-n-a-v-i-g-a-t-i-n-g_-v-a-r-b-i-t.md) | [Kraken API]<br>val [NAVIGATING_VARBIT](-n-a-v-i-g-a-t-i-n-g_-v-a-r-b-i-t.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [SAILING_CONTROLS_ID](-s-a-i-l-i-n-g_-c-o-n-t-r-o-l-s_-i-d.md) | [Kraken API]<br>val [SAILING_CONTROLS_ID](-s-a-i-l-i-n-g_-c-o-n-t-r-o-l-s_-i-d.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [SPEED_VARBIT](-s-p-e-e-d_-v-a-r-b-i-t.md) | [Kraken API]<br>val [SPEED_VARBIT](-s-p-e-e-d_-v-a-r-b-i-t.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |

## Functions

| Name | Summary |
|---|---|
| [decreaseSpeed](decrease-speed.md) | [Kraken API]<br>open fun [decreaseSpeed](decrease-speed.md)()<br>Decreases the speed of the boat in the game. |
| [getDirection](get-direction.md) | [Kraken API]<br>open fun [getDirection](get-direction.md)(): [SailingService.Direction](-direction/index.md)<br>Retrieves the current direction of the boat based on its angle in the game. |
| [increaseSpeed](increase-speed.md) | [Kraken API]<br>open fun [increaseSpeed](increase-speed.md)()<br>Increases the speed of the boat in the game. |
| [isMoving](is-moving.md) | [Kraken API]<br>open fun [isMoving](is-moving.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Checks if the boat is currently moving based on the speed state. |
| [isNavigating](is-navigating.md) | [Kraken API]<br>open fun [isNavigating](is-navigating.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Determines whether the player is at the helm of the boat and navigating  This method checks the value of a specific varbit to identify if the navigation system is active. |
| [setDirection](set-direction.md) | [Kraken API]<br>open fun [setDirection](set-direction.md)(direction: [SailingService.Direction](-direction/index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Sets the sailing direction of the boat in the game. |
| [setSails](set-sails.md) | [Kraken API]<br>open fun [setSails](set-sails.md)()<br>Deploys the boat's sails to enable sailing control. |
| [unsetSails](unset-sails.md) | [Kraken API]<br>open fun [unsetSails](unset-sails.md)()<br>Disables the boat's sails to bring the vessel to a halt. |
