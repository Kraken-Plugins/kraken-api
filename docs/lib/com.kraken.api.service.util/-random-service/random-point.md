//[lib](../../../index.md)/[com.kraken.api.service.util](../index.md)/[RandomService](index.md)/[randomPoint](random-point.md)

# randomPoint

[Kraken API]\
open fun [randomPoint](random-point.md)(mean: [Point](https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/java/awt/Point.html), maxRad: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), cutoff: [Double](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-double/index.html)): [Point](https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/java/awt/Point.html)

Generates a random point on the screen, weighted around a central point (mean) within a maximum radius. The point is selected to simulate human-like randomness in mouse movement or other actions.

#### Return

A random point near the central point, within the specified radius.

#### Parameters

Kraken API

| | |
|---|---|
| mean | The central point to weight the randomness around. |
| maxRad | The maximum radius away from the central point. |
| cutoff | The cutoff value for restricting extreme values. Defaults to GAUSS_CUTOFF(4) if less than or equal to 0. |

[Kraken API]\
open fun [randomPoint](random-point.md)(rect: [Rectangle](https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/java/awt/Rectangle.html), cutoff: [Double](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-double/index.html)): [Point](https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/java/awt/Point.html)

Generates a random point within the bounds of the given rectangle, biased towards the center. This method is useful for simulating human-like randomness in screen interactions.

#### Return

A random point within the rectangle, biased towards the middle.

#### Parameters

Kraken API

| | |
|---|---|
| rect | The rectangular area within which to generate the random point. |
| cutoff | The cutoff value for restricting extreme values. Defaults to GAUSS_CUTOFF(4) if less than or equal to 0. |
