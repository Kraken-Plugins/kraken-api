//[kraken-api](../../../index.md)/[com.kraken.api.query.npc](../index.md)/[NpcQuery](index.md)/[within](within.md)

# within

[Kraken API]\
open fun [within](within.md)(distance: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [NpcQuery](index.md)

Filters the NPCs in the query to include only those within a specified distance from the local player's position. 

 This method calculates the distance between each NPC's world location and the local player's current world location, including only those NPCs with a distance less than or equal to the specified value. 

#### Return

A filtered `NpcQuery` containing only the NPCs within the specified distance from the local player.

#### Parameters

Kraken API

| | |
|---|---|
| distance | The maximum distance (in tiles) from the local player within which NPCs should be included. This value must be greater than or equal to 0. |
