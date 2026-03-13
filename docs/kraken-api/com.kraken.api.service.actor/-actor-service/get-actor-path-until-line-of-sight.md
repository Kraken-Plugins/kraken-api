//[kraken-api](../../../index.md)/[com.kraken.api.service.actor](../index.md)/[ActorService](index.md)/[getActorPathUntilLineOfSight](get-actor-path-until-line-of-sight.md)

# getActorPathUntilLineOfSight

[Kraken API]\
open fun [getActorPathUntilLineOfSight](get-actor-path-until-line-of-sight.md)(npc: NPC): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;

Computes the movement path an NPC would take towards the local player and truncates it at the first tile where the NPC gains line of sight to the player.

#### Return

Path steps up to and including the first line-of-sight tile. Returns an empty list when line of sight is already available from the NPC's current tile.

#### Parameters

Kraken API

| | |
|---|---|
| npc | The source NPC. |

[Kraken API]\
open fun [getActorPathUntilLineOfSight](get-actor-path-until-line-of-sight.md)(npc: NPC, player: Player): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;

Computes the movement path an NPC would take towards a target player and truncates it at the first tile where the NPC gains line of sight to that player.

#### Return

Path steps up to and including the first line-of-sight tile. Returns an empty list when line of sight is already available from the NPC's current tile.

#### Parameters

Kraken API

| | |
|---|---|
| npc | The source NPC. |
| player | The target player. |
