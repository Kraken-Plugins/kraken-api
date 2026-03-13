//[kraken-api](../../../index.md)/[com.kraken.api.service.pathfinding](../index.md)/[LocalPathfinder](index.md)/[renderPath](render-path.md)

# renderPath

[Kraken API]\
open fun [renderPath](render-path.md)(path: [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;WorldPoint&gt;, graphics: [Graphics2D](https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/java/awt/Graphics2D.html), pathColor: [Color](https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/java/awt/Color.html))

Renders a series of tiles representing a path on the game canvas. This includes drawing connected lines between the tiles and optionally highlighting the last tile in the path. 

 The method uses the provided Graphics2D instance to draw on the screen and a Color to style the tiles.

#### Parameters

Kraken API

| | |
|---|---|
| path | The list of WorldPoint objects representing the path. Each point is rendered on the game canvas. |
| graphics | The Graphics2D instance used to render the path on the screen. |
| pathColor | The Color used to draw the tiles on the path. The last tile is highlighted in red with partial transparency. |
