//[kraken-api](../../../index.md)/[com.kraken.api.query.npc](../index.md)/[NpcQuery](index.md)/[interactingWith](interacting-with.md)

# interactingWith

[Kraken API]\
open fun [interactingWith](interacting-with.md)(actor: Actor): [NpcQuery](index.md)

Filters the query to include only NPCs that are currently interacting with the specified `actor`. 

 An NPC is considered to be interacting with the given actor if the NPC's `interacting` target is non-`null` and matches the provided `actor`. 

- This method is useful for identifying NPCs actively engaging with a specific actor, such as another player, NPC, or inanimate entity.

#### Return

A filtered `NpcQuery` containing only the NPCs that are interacting with the specified actor.

#### Parameters

Kraken API

| | |
|---|---|
| actor | The Actor that the NPCs being searched for should be interacting with. Passing `null` as the parameter is not allowed. |
