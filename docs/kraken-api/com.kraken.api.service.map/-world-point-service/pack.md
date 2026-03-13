//[kraken-api](../../../index.md)/[com.kraken.api.service.map](../index.md)/[WorldPointService](index.md)/[pack](pack.md)

# pack

[Kraken API]\
open fun [pack](pack.md)(wp: WorldPoint): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)

Compresses a WorldPoint into a single integer.

#### Return

the compressed WorldPoint

#### Parameters

Kraken API

| | |
|---|---|
| wp | the WorldPoint to compress |

[Kraken API]\
open fun [pack](pack.md)(x: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), y: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), z: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)

Compresses x, y, z coordinates into a single integer.

#### Return

the compressed coordinates

#### Parameters

Kraken API

| | |
|---|---|
| x | the x coordinate (0 - 16383) |
| y | the y coordinate (0 - 32767) |
| z | the z coordinate (0 - 7) |
