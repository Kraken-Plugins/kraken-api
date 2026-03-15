//[kraken-api](../../../index.md)/[com.kraken.api](../index.md)/[Context](index.md)/[groundItems](ground-items.md)

# groundItems

[Kraken API]\
open fun [groundItems](ground-items.md)(): [GroundObjectQuery](../../com.kraken.api.query.groundobject/-ground-object-query/index.md)

Creates a new query builder for Ground Items. GroundItems are items that exist on a tile that the player can pick up and store in their inventory. Examples include: bones dropped from an NPC or loot dropped by another player on a tile. Usage: `ctx.groundObjects().withName("Twisted Bow").nearest().interact("Take");`

#### Return

GroundObjectQuery used to chain together predicates to select specific ground items within the scene.
