//[lib](../../../index.md)/[com.kraken.api.service.actor](../index.md)/[ActorService](index.md)/[getActorLineOfSightTerminationTile](get-actor-line-of-sight-termination-tile.md)

# getActorLineOfSightTerminationTile

[Kraken API]\
open fun [getActorLineOfSightTerminationTile](get-actor-line-of-sight-termination-tile.md)(npc: NPC): WorldPoint

Finds the tile where an NPC path would terminate once the NPC has line of sight to the local player. If line of sight is already available, the NPC's current tile is returned.

#### Return

The termination tile, or null when inputs are invalid.

#### Parameters

Kraken API

| | |
|---|---|
| npc | The source NPC. |

[Kraken API]\
open fun [getActorLineOfSightTerminationTile](get-actor-line-of-sight-termination-tile.md)(npc: NPC, player: Player): WorldPoint

Finds the tile where an NPC path would terminate once the NPC has line of sight to the target player. If line of sight is already available, the NPC's current tile is returned.

#### Return

The termination tile, or null when inputs are invalid.

#### Parameters

Kraken API

| | |
|---|---|
| npc | The source NPC. |
| player | The target player. |
