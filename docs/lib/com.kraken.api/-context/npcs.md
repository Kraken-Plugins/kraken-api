//[lib](../../../index.md)/[com.kraken.api](../index.md)/[Context](index.md)/[npcs](npcs.md)

# npcs

[Kraken API]\
open fun [npcs](npcs.md)(): [NpcQuery](../../com.kraken.api.query.npc/-npc-query/index.md)

Creates a new query builder for NPCs. Usage: ctx.npcs().withName(&quot;Goblin&quot;).first().interact(&quot;Attack&quot;);

#### Return

NpcQuery object used to chain together predicates to select specific NPC's within the scene.
