//[kraken-api](../../../index.md)/[com.kraken.api](../index.md)/[Context](index.md)/[gameObjects](game-objects.md)

# gameObjects

[Kraken API]\
open fun [gameObjects](game-objects.md)(): [GameObjectQuery](../../com.kraken.api.query.gameobject/-game-object-query/index.md)

Creates a new query builder for game objects. Game objects are objects in the game world like: Trees, ore, or fishing spots which exist on tiles, can be interacted with, but cannot be picked up by the player. Usage: `ctx.gameObjects().withName("Oak Tree").nearest().interact("Chop");`

#### Return

GameObjectQuery used to chain together predicates to select specific game objects within the scene.
