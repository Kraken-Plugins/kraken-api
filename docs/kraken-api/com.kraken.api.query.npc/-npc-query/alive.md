//[kraken-api](../../../index.md)/[com.kraken.api.query.npc](../index.md)/[NpcQuery](index.md)/[alive](alive.md)

# alive

[Kraken API]\
open fun [alive](alive.md)(): [NpcQuery](index.md)

Filters the query to include only NPCs that are currently alive. 

 An NPC is considered to be alive if its internal state indicates it is not dead. This method applies a filter to exclude NPCs marked as dead from the result set. 

- NPCs included in the resulting query are capable of interaction or action within the game environment.
- This filter helps narrow the query to focus only on viable, active NPCs.

#### Return

A filtered `NpcQuery` containing only the NPCs that are alive.
