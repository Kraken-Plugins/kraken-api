//[lib](../../../index.md)/[com.kraken.api.service](../index.md)/[SailingService](index.md)/[unsetSails](unset-sails.md)

# unsetSails

[Kraken API]\
open fun [unsetSails](unset-sails.md)()

Disables the boat's sails to bring the vessel to a halt. 

This method initiates the in-game action to unset the boat's sails, effectively stopping the boat's movement. It checks the current movement status of the boat by invoking [isMoving](is-moving.md). If the boat is stationary, the method exits immediately without issuing any commands. This safeguards against unnecessary actions when the boat is not in motion.
