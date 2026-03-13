//[kraken-api](../../../index.md)/[com.kraken.api.query.npc](../index.md)/[NpcEntity](index.md)/[getDistanceFromPlayer](get-distance-from-player.md)

# getDistanceFromPlayer

[Kraken API]\
open fun [getDistanceFromPlayer](get-distance-from-player.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)

Calculates the distance between the NPC and the local player within the game world. 

 The method retrieves the NPC's local location and the local player's location from the game client, then computes the distance between the two positions. If the distance cannot be calculated (e.g., due to a timeout on the client thread), `Integer.MAX_VALUE` is returned as a fallback. 

- The computation is performed on the game's client thread to ensure thread safety unless the result is unavailable.

#### Return

The distance between the NPC's location and the local player's location in the game world, or `Integer.MAX_VALUE` if the distance cannot be determined.
