//[lib](../../../index.md)/[com.kraken.api.service.map](../index.md)/[WorldMapService](index.md)/[worldPointToMapPointX](world-point-to-map-point-x.md)

# worldPointToMapPointX

[Kraken API]\
open fun [worldPointToMapPointX](world-point-to-map-point-x.md)(worldPoint: WorldPoint): [Integer](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Integer.html)

Converts a `WorldPoint` to its corresponding screen X coordinate on the world map. 

This method calculates the X coordinate of a given `WorldPoint` relative to the world map interface, considering the map's zoom level, current position, and bounds. If the world map or any related components are unavailable, the method returns `null`.

#### Return

the screen X coordinate on the world map, or `null` if the conversion cannot be performed

#### Parameters

Kraken API

| | |
|---|---|
| worldPoint | the `WorldPoint` to convert; must not be `null` |
