//[lib](../../../index.md)/[com.kraken.api.service.util](../index.md)/[RandomService](index.md)/[randomPointEx](random-point-ex.md)

# randomPointEx

[Kraken API]\
open fun [randomPointEx](random-point-ex.md)(from: [Point](https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/java/awt/Point.html), rect: [Rectangle](https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/java/awt/Rectangle.html), force: [Double](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-double/index.html)): [Point](https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/java/awt/Point.html)

Generates a random point within the bounds of a rectangle, skewed towards a specified 'from' point. Useful for simulating more human-like randomness in actions such as dragging or moving the mouse.

#### Return

A random point within the rectangle, skewed towards the 'from' point.

#### Parameters

Kraken API

| | |
|---|---|
| from | The point to bias the random point generation towards. |
| rect | The rectangular area within which to generate the random point. |
| force | A multiplier that defines how strongly the point should be skewed towards the 'from' point. |
