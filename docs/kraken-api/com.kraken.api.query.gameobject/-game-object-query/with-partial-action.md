//[kraken-api](../../../index.md)/[com.kraken.api.query.gameobject](../index.md)/[GameObjectQuery](index.md)/[withPartialAction](with-partial-action.md)

# withPartialAction

[Kraken API]\
open fun [withPartialAction](with-partial-action.md)(actionSubstring: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)): [GameObjectQuery](index.md)

Filters the game objects to include only those with at least one action containing the specified substring. 

 This method is case-insensitive and matches the specified substring against all non-null action strings associated with the game object. 

#### Return

A `GameObjectQuery` with the applied filter to include only objects with actions matching the specified substring.

#### Parameters

Kraken API

| | |
|---|---|
| actionSubstring | The substring to search for within the actions of the game objects. Must not be `null`. |
