//[lib](../../../index.md)/[com.kraken.api.service.tile](../index.md)/[GameArea](index.md)

# GameArea

[Kraken API]\
open class [GameArea](index.md)

Encapsulates a set of tiles representing an area. Provides helper methods for checking containment, retrieval, and visualization.

## Constructors

| | |
|---|---|
| [GameArea](-game-area.md) | [Kraken API]<br>constructor() |

## Functions

| Name | Summary |
|---|---|
| [contains](contains.md) | [Kraken API]<br>open fun [contains](contains.md)(point: WorldPoint): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Checks if the given world point is inside this area. |
| [getRandomTile](get-random-tile.md) | [Kraken API]<br>open fun [getRandomTile](get-random-tile.md)(): WorldPoint<br>Returns a random tile from the area. |
| [render](render.md) | [Kraken API]<br>open fun [render](render.md)(client: Client, graphics: [Graphics2D](https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/java/awt/Graphics2D.html), color: [Color](https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/java/awt/Color.html), outline: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html))<br>Visualizes the area on the game screen. |
| [renderMinimap](render-minimap.md) | [Kraken API]<br>open fun [renderMinimap](render-minimap.md)(client: Client, graphics: [Graphics2D](https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/java/awt/Graphics2D.html), color: [Color](https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/java/awt/Color.html))<br>Renders the area on the minimap. |
| [subtract](subtract.md) | [Kraken API]<br>open fun [subtract](subtract.md)(other: [GameArea](index.md)): [GameArea](index.md)<br>Removes the tiles of another area from this one (Difference). |
| [union](union.md) | [Kraken API]<br>open fun [union](union.md)(other: [GameArea](index.md)): [GameArea](index.md)<br>Combines this area with another (Union). |
