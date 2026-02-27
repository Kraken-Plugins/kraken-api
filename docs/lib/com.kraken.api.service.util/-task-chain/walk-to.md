//[lib](../../../index.md)/[com.kraken.api.service.util](../index.md)/[TaskChain](index.md)/[walkTo](walk-to.md)

# walkTo

[Kraken API]\
open fun [walkTo](walk-to.md)(target: WorldPoint): [TaskChain](index.md)

Walks to a specific WorldPoint using the LocalPathfinder and MovementService. 

 This method utilizes `findSparsePath` to calculate an efficient route and processes the path sequentially. It will wait for the player to reach each waypoint (within 1 tile) before proceeding to the next. 

#### Return

The current TaskChain instance.

#### Parameters

Kraken API

| | |
|---|---|
| target | The destination WorldPoint. |

[Kraken API]\
open fun [walkTo](walk-to.md)(target: WorldPoint, radius: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [TaskChain](index.md)

Walks to an approximate location within a radius of the target. Useful for banking or interacting with large objects where exact tile precision isn't needed.

#### Return

The current TaskChain instance.

#### Parameters

Kraken API

| | |
|---|---|
| target | The center WorldPoint. |
| radius | The radius to search for a walkable tile. |

[Kraken API]\
open fun [walkTo](walk-to.md)(area: WorldArea): [TaskChain](index.md)

Walks to a random reachable point inside a specific WorldArea.

#### Return

The current TaskChain instance.

#### Parameters

Kraken API

| | |
|---|---|
| area | The WorldArea to walk into. |
