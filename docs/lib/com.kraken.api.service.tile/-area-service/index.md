//[lib](../../../index.md)/[com.kraken.api.service.tile](../index.md)/[AreaService](index.md)

# AreaService

[Kraken API]\
open class [AreaService](index.md)

## Constructors

| | |
|---|---|
| [AreaService](-area-service.md) | [Kraken API]<br>constructor() |

## Functions

| Name | Summary |
|---|---|
| [createAreaFromRadius](create-area-from-radius.md) | [Kraken API]<br>open fun [createAreaFromRadius](create-area-from-radius.md)(center: WorldPoint, radius: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [GameArea](../-game-area/index.md)<br>Creates a square area around a center point. |
| [createFromPoints](create-from-points.md) | [Kraken API]<br>open fun [createFromPoints](create-from-points.md)(points: [Collection](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/Collection.html)&lt;WorldPoint&gt;): [GameArea](../-game-area/index.md)<br>Creates an area from a raw collection of points. |
| [createPolygonArea](create-polygon-area.md) | [Kraken API]<br>open fun [createPolygonArea](create-polygon-area.md)(vertices: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;WorldPoint&gt;): [GameArea](../-game-area/index.md)<br>Creates a complex shape (e.g., L-shape) from vertices.<br>[Kraken API]<br>open fun [createPolygonArea](create-polygon-area.md)(vertices: [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;): [GameArea](../-game-area/index.md)<br>Creates a complex shape from a list of vertices. |
| [createReachableArea](create-reachable-area.md) | [Kraken API]<br>open fun [createReachableArea](create-reachable-area.md)(center: WorldPoint, range: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), ignoreCollision: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)): [GameArea](../-game-area/index.md)<br>Creates an area based on movement reachability (BFS). |
