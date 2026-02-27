//[lib](../../../index.md)/[com.kraken.api.query.npc](../index.md)/[NpcQuery](index.md)/[nearest](nearest.md)

# nearest

[Kraken API]\
open fun [nearest](nearest.md)(): [NpcEntity](../-npc-entity/index.md)

Retrieves the nearest NPC entity to the local player's current position. 

 This method determines the NPC closest to the local player by comparing the distances between each NPC's local location and the local player's local location. The comparison is performed by sorting the NPCs based on their proximity, and the first (closest) NPC is selected. 

 The result is typically used to quickly identify and interact with the most immediate NPC relative to the player's current position, which can assist in various gameplay interactions. 

#### Return

The `NpcEntity` nearest to the local player's current position, as determined based on the shortest distance in the local coordinate system. If no NPCs are available, the return value may be `null`.
