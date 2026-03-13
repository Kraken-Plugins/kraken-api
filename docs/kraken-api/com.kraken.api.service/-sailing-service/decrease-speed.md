//[kraken-api](../../../index.md)/[com.kraken.api.service](../index.md)/[SailingService](index.md)/[decreaseSpeed](decrease-speed.md)

# decreaseSpeed

[Kraken API]\
open fun [decreaseSpeed](decrease-speed.md)()

Decreases the speed of the boat in the game. 

This method interacts with the in-game sailing controls to reduce the boat's speed by one level, unless the current speed is already at the lowest setting. The speed is determined by querying the value of a specific varbit used to represent the boat's speed state.

If the boat's speed is set to the minimum value (3 in the current configuration), the method exits immediately without issuing any further actions.
