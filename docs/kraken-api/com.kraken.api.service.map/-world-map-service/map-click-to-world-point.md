//[kraken-api](../../../index.md)/[com.kraken.api.service.map](../index.md)/[WorldMapService](index.md)/[mapClickToWorldPoint](map-click-to-world-point.md)

# mapClickToWorldPoint

[Kraken API]\
open fun [mapClickToWorldPoint](map-click-to-world-point.md)(clickX: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), clickY: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): WorldPoint

Converts the screen-space click coordinates on the world map interface to a corresponding `WorldPoint` in the game world. 

This method takes the x and y coordinates of a mouse click and determines the equivalent world map coordinates by accounting for the map's zoom level, current position, and bounds. If the click is outside the map's bounds or the world map is unavailable, the method returns `null`.

#### Return

the `WorldPoint` corresponding to the clicked coordinates, or `null` if the conversion cannot be performed

#### Parameters

Kraken API

| | |
|---|---|
| clickX | the x-coordinate of the mouse click on the world map interface |
| clickY | the y-coordinate of the mouse click on the world map interface |

[Kraken API]\
open fun [mapClickToWorldPoint](map-click-to-world-point.md)(clickX: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), clickY: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), plane: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): WorldPoint

Maps the click coordinates on the world map to a `WorldPoint` in the game world, with a specified plane (altitude). 

This method converts user interaction on the world map interface (e.g., clicking) into corresponding game world coordinates while ensuring the plane value is constrained between valid ranges (0 to 3). If the conversion fails (e.g., invalid input or map state), `null` is returned.

#### Return

the `WorldPoint` corresponding to the click coordinates and specified plane, or `null` if the conversion fails

#### Parameters

Kraken API

| | |
|---|---|
| clickX | the x-coordinate of the mouse click on the world map interface |
| clickY | the y-coordinate of the mouse click on the world map interface |
| plane | the desired plane (altitude) for the resulting `WorldPoint`; values outside the range [0, 3] will be clamped |
