//[kraken-api](../../../index.md)/[com.kraken.api.service.map](../index.md)/[WorldMapService](index.md)/[isWorldMapOpen](is-world-map-open.md)

# isWorldMapOpen

[Kraken API]\
open fun [isWorldMapOpen](is-world-map-open.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Determines whether the world map interface is currently open and visible. 

This method checks the state of the world map widget within the game client to assess its visibility. If the widget corresponding to the world map is not available or is hidden, it returns `false`, indicating that the world map is closed or inaccessible. Otherwise, it returns `true`.

#### Return

`true` if the world map interface is open and visible; `false` otherwise. This includes cases where the widget is absent or hidden.
