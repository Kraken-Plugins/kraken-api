//[kraken-api](../../../index.md)/[com.kraken.api.query.world](../index.md)/[WorldQuery](index.md)/[next](next.md)

# next

[Kraken API]\
open fun [next](next.md)(): [WorldEntity](../-world-entity/index.md)

Retrieves the next WorldEntity in the sorted list whose world ID is greater than the current world ID. If no such entity exists, the method returns the first WorldEntity in the list. If the list is empty, it returns `null`. 

This method sorts the available WorldEntity objects in ascending order by their world ID and selects the first one with an ID greater than the current world ID as determined by the client's state.

#### Return

The next WorldEntity with a world ID greater than the current world ID, the first WorldEntity in the list if no ID is greater, or `null` if the list is empty.
