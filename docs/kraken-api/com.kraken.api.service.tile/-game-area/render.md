//[kraken-api](../../../index.md)/[com.kraken.api.service.tile](../index.md)/[GameArea](index.md)/[render](render.md)

# render

[Kraken API]\
open fun [render](render.md)(client: Client, graphics: [Graphics2D](https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/java/awt/Graphics2D.html), color: [Color](https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/java/awt/Color.html), outline: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html))

Visualizes the area on the game screen. This should be called from a RuneLite Overlay's `render()` method.

#### Parameters

Kraken API

| | |
|---|---|
| client | an instance of the game client |
| graphics | The graphics context |
| color | The fill color (alpha is handled automatically if needed, but best to pass a translucent color) |
| outline | Whether to draw just the outline or fill the tiles |
