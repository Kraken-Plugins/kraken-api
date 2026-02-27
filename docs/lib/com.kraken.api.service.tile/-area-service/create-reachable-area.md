//[lib](../../../index.md)/[com.kraken.api.service.tile](../index.md)/[AreaService](index.md)/[createReachableArea](create-reachable-area.md)

# createReachableArea

[Kraken API]\
open fun [createReachableArea](create-reachable-area.md)(center: WorldPoint, range: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), ignoreCollision: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)): [GameArea](../-game-area/index.md)

Creates an area based on movement reachability (BFS). Uses your existing TileService logic.

#### Return

GameArea the game area containing the WorldPoints within the radius

#### Parameters

Kraken API

| | |
|---|---|
| center | The center of the reachable area |
| range | The range that the reachable area should extend to |
| ignoreCollision | True if collision maps should be ignored when generating the game area |
