//[lib](../../../index.md)/[com.kraken.api.service.actor](../index.md)/[ActorService](index.md)/[getLineOfSightTiles](get-line-of-sight-tiles.md)

# getLineOfSightTiles

[Kraken API]\
open fun [getLineOfSightTiles](get-line-of-sight-tiles.md)(npc: NPC, range: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;

Retrieves a list of all tiles within a specified radius that an NPC currently has line of sight to. 

#### Return

A list of WorldPoints representing visible tiles. Returns an empty list if none are found.

#### Parameters

Kraken API

| | |
|---|---|
| npc | The source NPC. |
| range | The radius to check for visible tiles. |
