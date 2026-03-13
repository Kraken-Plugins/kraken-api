//[kraken-api](../../../index.md)/[com.kraken.api.service.map](../index.md)/[WorldMapService](index.md)

# WorldMapService

[Kraken API]\
open class [WorldMapService](index.md)

## Constructors

| | |
|---|---|
| [WorldMapService](-world-map-service.md) | [Kraken API]<br>constructor() |

## Functions

| Name | Summary |
|---|---|
| [isWorldMapOpen](is-world-map-open.md) | [Kraken API]<br>open fun [isWorldMapOpen](is-world-map-open.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Determines whether the world map interface is currently open and visible. |
| [mapClickToWorldPoint](map-click-to-world-point.md) | [Kraken API]<br>open fun [mapClickToWorldPoint](map-click-to-world-point.md)(clickX: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), clickY: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): WorldPoint<br>Converts the screen-space click coordinates on the world map interface to a corresponding `WorldPoint` in the game world.<br>[Kraken API]<br>open fun [mapClickToWorldPoint](map-click-to-world-point.md)(clickX: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), clickY: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), plane: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): WorldPoint<br>Maps the click coordinates on the world map to a `WorldPoint` in the game world, with a specified plane (altitude). |
| [worldPointToMapPointX](world-point-to-map-point-x.md) | [Kraken API]<br>open fun [worldPointToMapPointX](world-point-to-map-point-x.md)(worldPoint: WorldPoint): [Integer](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Integer.html)<br>Converts a `WorldPoint` to its corresponding screen X coordinate on the world map. |
| [worldPointToMapPointY](world-point-to-map-point-y.md) | [Kraken API]<br>open fun [worldPointToMapPointY](world-point-to-map-point-y.md)(worldPoint: WorldPoint): [Integer](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Integer.html)<br>Converts a `WorldPoint` in the game world to its corresponding screen Y coordinate on the world map interface. |
