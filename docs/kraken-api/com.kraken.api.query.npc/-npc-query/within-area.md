//[kraken-api](../../../index.md)/[com.kraken.api.query.npc](../index.md)/[NpcQuery](index.md)/[withinArea](within-area.md)

# withinArea

[Kraken API]\
open fun [withinArea](within-area.md)(min: WorldPoint, max: WorldPoint): [NpcQuery](index.md)

Filters the query to include only NPCs that are located within a specified rectangular area. The area is defined by two corner points, `min` (lower-left) and `max` (upper-right), in the world map grid. 

 NPCs are included in the resulting query if their world point lies within the bounds created by the two corner points. The bounds are inclusive of the edges. This allows querying NPCs that exist within a specific area of interest. 

#### Return

A filtered `NpcQuery` containing only the NPCs located within the specified area.

#### Parameters

Kraken API

| | |
|---|---|
| min | The WorldPoint representing the lower-left corner of the area. This defines one bound of the rectangular query range. |
| max | The WorldPoint representing the upper-right corner of the area. This defines the opposite bound of the rectangular query range. |
