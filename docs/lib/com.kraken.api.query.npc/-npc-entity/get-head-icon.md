//[lib](../../../index.md)/[com.kraken.api.query.npc](../index.md)/[NpcEntity](index.md)/[getHeadIcon](get-head-icon.md)

# getHeadIcon

[Kraken API]\
open fun [getHeadIcon](get-head-icon.md)(): HeadIcon

Retrieves the head icon associated with the NPC, if it exists. 

 A head icon represents an overhead visual indicator, such as combat prayers or effects like Hunllef's prayers or Nex's deflect melee. This is determined from the NPC's overhead sprite IDs. 

- If no head icons are defined for the NPC, this will return `null`.
- If a valid head icon is found, it will be returned as a `HeadIcon` enum.

#### Return

The `HeadIcon` for the NPC, or `null` if no valid head icon exists.
