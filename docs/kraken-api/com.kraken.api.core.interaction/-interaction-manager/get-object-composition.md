//[kraken-api](../../../index.md)/[com.kraken.api.core.interaction](../index.md)/[InteractionManager](index.md)/[getObjectComposition](get-object-composition.md)

# getObjectComposition

[Kraken API]\
open fun [getObjectComposition](get-object-composition.md)(client: Client, object: TileObject): ObjectComposition

Retrieves the ObjectComposition of the specified TileObject. 

 This method runs on the client thread to ensure safe access to client state. It first retrieves the ObjectComposition corresponding to the object's ID. If the composition has been transformed (via impostor), the transformed ObjectComposition is returned; otherwise, the original composition is returned.

#### Return

the ObjectComposition of the given TileObject, or `null` if no composition could be retrieved or determined.

#### Parameters

Kraken API

| | |
|---|---|
| client | the Client instance used to interact with the game state. |
| object | the TileObject whose ObjectComposition is to be fetched. The TileObject must have a valid ID associated with it. |
