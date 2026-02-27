//[lib](../../../index.md)/[com.kraken.api.service.map](../index.md)/[WorldMapService](index.md)/[worldPointToMapPointY](world-point-to-map-point-y.md)

# worldPointToMapPointY

[Kraken API]\
open fun [worldPointToMapPointY](world-point-to-map-point-y.md)(worldPoint: WorldPoint): [Integer](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Integer.html)

Converts a `WorldPoint` in the game world to its corresponding screen Y coordinate on the world map interface. 

This method calculates the screen Y position of the given `WorldPoint` in relation to the world map widget, taking into account the map's zoom level, current position, and widget bounds. If the `WorldPoint` or world map components are unavailable, the method returns `null`.

#### Return

the screen Y coordinate on the world map corresponding to the given `WorldPoint`, or `null` if the conversion cannot be performed due to invalid input or unavailable map state.

#### Parameters

Kraken API

| | |
|---|---|
| worldPoint | the `WorldPoint` to convert; must not be `null`. If `null`, the method will return `null`. |
