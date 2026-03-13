//[kraken-api](../../../index.md)/[com.kraken.api.query.npc](../index.md)/[NpcQuery](index.md)/[withAction](with-action.md)

# withAction

[Kraken API]\
open fun [withAction](with-action.md)(action: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)): [NpcQuery](index.md)

Filters the query for NPCs that have a specific menu option available. 

 This method checks the list of actions associated with each NPC's composition. If the specified `option` matches any of the available actions (case-insensitive), the NPC will be included in the resulting query. 

#### Return

A filtered `NpcQuery` containing only the NPCs that match the specified menu option.

#### Parameters

Kraken API

| | |
|---|---|
| action | The menu option to check for, e.g., @Attack, @Talk-to, @Use, etc... The input is case-insensitive. |
