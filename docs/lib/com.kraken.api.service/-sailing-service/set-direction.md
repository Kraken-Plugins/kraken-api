//[lib](../../../index.md)/[com.kraken.api.service](../index.md)/[SailingService](index.md)/[setDirection](set-direction.md)

# setDirection

[Kraken API]\
open fun [setDirection](set-direction.md)(direction: [SailingService.Direction](-direction/index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Sets the sailing direction of the boat in the game. 

 This method first ensures that the necessary game packets are loaded. If not, it immediately returns false. If packets are loaded, a mouse click packet is queued, followed by a request to update the boat direction using the provided [Direction](-direction/index.md) value. 

The direction is set by passing the direction's code to the sailing packet system. Directions are represented using predefined constants in the [Direction](-direction/index.md) enumeration. 

#### Return

true if the direction was successfully set; false if the necessary game packets were not loaded.

#### Parameters

Kraken API

| | |
|---|---|
| direction | The desired [Direction](-direction/index.md) to set the boat's heading. Directions are enumerated values ranging from SOUTH(0) to SOUTH_SOUTH_EAST(15). |
