//[kraken-api](../../../index.md)/[com.kraken.api.query.npc](../index.md)/[NpcQuery](index.md)/[attackable](attackable.md)

# attackable

[Kraken API]\
open fun [attackable](attackable.md)(): [NpcQuery](index.md)

Returns Attackable NPC within the scene. NPC's are considered attackable when: - They are not dead - Their menu options contain an &quot;Attack&quot; option 

 Combat level is not taken into consideration since there are many NPC's without a combat level that are attackable. i.e. Yama's void flares

#### Return

NpcQuery
