//[kraken-api](../../../index.md)/[com.kraken.api.service](../index.md)/[SailingService](index.md)/[getDirection](get-direction.md)

# getDirection

[Kraken API]\
open fun [getDirection](get-direction.md)(): [SailingService.Direction](-direction/index.md)

Retrieves the current direction of the boat based on its angle in the game. 

 This method utilizes the [fromAngle](-direction/from-angle.md) method to derive the boat's direction from the angle value provided by the `VarbitID.SAILING_BOAT_SPAWNED_ANGLE` varbit. The angle is divided into predefined directional constants representing the boat's heading. 

#### Return

The [Direction](-direction/index.md) the boat is currently facing. If no valid direction can be derived, `null` is returned.
