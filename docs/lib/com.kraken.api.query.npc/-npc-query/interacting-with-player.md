//[lib](../../../index.md)/[com.kraken.api.query.npc](../index.md)/[NpcQuery](index.md)/[interactingWithPlayer](interacting-with-player.md)

# interactingWithPlayer

[Kraken API]\
open fun [interactingWithPlayer](interacting-with-player.md)(): [NpcQuery](index.md)

Filters the query to include only NPCs that are currently interacting with the local player. 

 An NPC is considered to be interacting with the local player if the NPC's `interacting` target is non-`null` and matches the client’s local player. 

- This method allows narrowing down the query to find NPCs that are actively engaged with the local player, whether by combat, dialogue, or other forms of interaction.

#### Return

A filtered `NpcQuery` containing only the NPCs that are interacting with the local player.
