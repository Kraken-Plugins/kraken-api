//[lib](../../../index.md)/[com.kraken.api.query.gameobject](../index.md)/[GameObjectQuery](index.md)/[nameContains](name-contains.md)

# nameContains

[Kraken API]\
open fun [nameContains](name-contains.md)(name: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)): [GameObjectQuery](index.md)

Filters the stream of game objects for objects which match a specific substring of a name. For example: `ctx.gameObjects().nameContains("Oak")` will find Oak tree game objects in the scene.

#### Return

GameObjectQuery

#### Parameters

Kraken API

| | |
|---|---|
| name | The name to match against |
