//[kraken-api](../../../index.md)/[com.kraken.api.query.npc](../index.md)/[NpcQuery](index.md)/[nearestTo](nearest-to.md)

# nearestTo

[Kraken API]\
open fun [nearestTo](nearest-to.md)(location: WorldPoint): [NpcQuery](index.md)

Sorts the current query results by determining the distance of each NPC's `WorldPoint` to a specified `WorldPoint` location and arranging them in ascending order of proximity. 

 This method calculates the distance between the provided `location` and each NPC's world location. The resulting query will contain NPCs sorted such that those closest to the specified location appear first. 

#### Return

A `NpcQuery` containing NPCs sorted by their proximity to the specified `location`.

#### Parameters

Kraken API

| | |
|---|---|
| location | The WorldPoint to which NPCs' distances will be calculated. This parameter defines the point of reference for sorting NPCs by proximity. |
