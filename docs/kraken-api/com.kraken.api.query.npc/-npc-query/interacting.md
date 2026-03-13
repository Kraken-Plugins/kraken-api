//[kraken-api](../../../index.md)/[com.kraken.api.query.npc](../index.md)/[NpcQuery](index.md)/[interacting](interacting.md)

# interacting

[Kraken API]\
open fun [interacting](interacting.md)(): [NpcQuery](index.md)

Filters the query to include only NPCs that are currently interacting with any entity other than the local player. 

 An NPC is considered to be interacting if its `interacting` target is non-`null` and does not match the local player. This includes NPCs that are engaged with other players, NPCs, or other entities in any form of interaction (e.g., combat, dialogue, etc.). 

- This method helps identify NPCs that are actively engaged in an interaction within the game world, excluding those interacting directly with the local player.

#### Return

A filtered `NpcQuery` containing only the NPCs that are interacting with entities other than the local player.
