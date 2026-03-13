//[kraken-api](../../../index.md)/[com.kraken.api.query.npc](../index.md)/[NpcQuery](index.md)/[reachable](reachable.md)

# reachable

[Kraken API]\
open fun [reachable](reachable.md)(): [NpcQuery](index.md)

Filters the NPCs to include only those that are reachable based on their world location. 

 This method applies a filter to the NPC query, ensuring that each NPC's raw data existence is validated and their world location is checked for reachability using the tile service. 

#### Return

A @NpcQuery containing only the NPCs that are reachable.
