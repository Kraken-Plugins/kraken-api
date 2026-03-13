//[kraken-api](../../../index.md)/[com.kraken.api.service.ui](../index.md)/[UIService](index.md)/[getClickingPoint](get-clicking-point.md)

# getClickingPoint

[Kraken API]\
open fun [getClickingPoint](get-clicking-point.md)(rectangle: [Rectangle](https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/java/awt/Rectangle.html), randomize: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)): [Point](https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/java/awt/Point.html)

Calculates a click point from a rectangle with optional randomization. When randomized, uses RandomService.randomPointEx with a distribution factor of 0.78.

#### Return

a point within the rectangle (randomized or centered)

#### Parameters

Kraken API

| | |
|---|---|
| rectangle | the rectangle to calculate the point from |
| randomize | whether to randomize the point within the rectangle |
