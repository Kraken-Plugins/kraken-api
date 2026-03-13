//[kraken-api](../../../index.md)/[com.kraken.api.query.world](../index.md)/[WorldQuery](index.md)/[previous](previous.md)

# previous

[Kraken API]\
open fun [previous](previous.md)(): [WorldEntity](../-world-entity/index.md)

Retrieves the previous World in the sorted list of worlds based on the current world number. The worlds are sorted in ascending order by their world number. 

 If a world with a smaller number than the current world is found, it is returned. Otherwise, the last world in the sorted list is returned. If the list of worlds is empty, `null` is returned. 

#### Return

the previous World in the sorted list, the last World if no smaller world exists, or `null` if the world list is empty.
