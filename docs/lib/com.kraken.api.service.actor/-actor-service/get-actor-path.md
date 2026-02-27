//[lib](../../../index.md)/[com.kraken.api.service.actor](../index.md)/[ActorService](index.md)/[getActorPath](get-actor-path.md)

# getActorPath

[Kraken API]\
open fun [getActorPath](get-actor-path.md)(npc: NPC): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;

Computes the movement path an NPC would take towards the local player using local collision data. The returned list does not include the NPC's current tile.

#### Return

The predicted step-by-step path toward the local player.

#### Parameters

Kraken API

| | |
|---|---|
| npc | The source NPC. |

[Kraken API]\
open fun [getActorPath](get-actor-path.md)(actor: Actor, player: Player): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;

Computes the movement path an actor would take towards a target player using local collision data. The returned list does not include the actor's current tile.

#### Return

The predicted step-by-step path.

#### Parameters

Kraken API

| | |
|---|---|
| actor | The source actor. |
| player | The target player. |

[Kraken API]\
open fun [getActorPath](get-actor-path.md)(actor: Actor, destination: WorldPoint): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;

Computes the movement path an actor would take towards a destination tile using local collision data. The returned list does not include the actor's current tile.

#### Return

The predicted step-by-step path.

#### Parameters

Kraken API

| | |
|---|---|
| actor | The source actor. |
| destination | The destination world tile. |
