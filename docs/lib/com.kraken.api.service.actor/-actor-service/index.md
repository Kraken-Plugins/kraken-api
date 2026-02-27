//[lib](../../../index.md)/[com.kraken.api.service.actor](../index.md)/[ActorService](index.md)

# ActorService

[Kraken API]\
open class [ActorService](index.md)

Utility service for calculating line of sight (LoS), collision, and reachability for actors and tiles within the game world.

## Constructors

| | |
|---|---|
| [ActorService](-actor-service.md) | [Kraken API]<br>constructor() |

## Functions

| Name | Summary |
|---|---|
| [getActorLineOfSightTerminationTile](get-actor-line-of-sight-termination-tile.md) | [Kraken API]<br>open fun [getActorLineOfSightTerminationTile](get-actor-line-of-sight-termination-tile.md)(npc: NPC): WorldPoint<br>Finds the tile where an NPC path would terminate once the NPC has line of sight to the local player.<br>[Kraken API]<br>open fun [getActorLineOfSightTerminationTile](get-actor-line-of-sight-termination-tile.md)(npc: NPC, player: Player): WorldPoint<br>Finds the tile where an NPC path would terminate once the NPC has line of sight to the target player. |
| [getActorPath](get-actor-path.md) | [Kraken API]<br>open fun [getActorPath](get-actor-path.md)(npc: NPC): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;<br>Computes the movement path an NPC would take towards the local player using local collision data.<br>[Kraken API]<br>open fun [getActorPath](get-actor-path.md)(actor: Actor, player: Player): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;<br>Computes the movement path an actor would take towards a target player using local collision data.<br>[Kraken API]<br>open fun [getActorPath](get-actor-path.md)(actor: Actor, destination: WorldPoint): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;<br>Computes the movement path an actor would take towards a destination tile using local collision data. |
| [getActorPathUntilLineOfSight](get-actor-path-until-line-of-sight.md) | [Kraken API]<br>open fun [getActorPathUntilLineOfSight](get-actor-path-until-line-of-sight.md)(npc: NPC): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;<br>Computes the movement path an NPC would take towards the local player and truncates it at the first tile where the NPC gains line of sight to the player.<br>[Kraken API]<br>open fun [getActorPathUntilLineOfSight](get-actor-path-until-line-of-sight.md)(npc: NPC, player: Player): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;<br>Computes the movement path an NPC would take towards a target player and truncates it at the first tile where the NPC gains line of sight to that player. |
| [getLineOfSightTiles](get-line-of-sight-tiles.md) | [Kraken API]<br>open fun [getLineOfSightTiles](get-line-of-sight-tiles.md)(npc: NPC, range: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;<br>Retrieves a list of all tiles within a specified radius that an NPC currently has line of sight to. |
| [hasLineOfSightTo](has-line-of-sight-to.md) | [Kraken API]<br>open fun [hasLineOfSightTo](has-line-of-sight-to.md)(source: Tile, other: Tile): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Checks if there is a clear line of sight between two scene tiles.<br>[Kraken API]<br>open fun [hasLineOfSightTo](has-line-of-sight-to.md)(source: WorldPoint, other: WorldPoint): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Checks if there is a clear line of sight between two world points. |
