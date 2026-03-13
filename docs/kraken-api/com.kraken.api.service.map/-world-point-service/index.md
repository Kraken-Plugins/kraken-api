//[kraken-api](../../../index.md)/[com.kraken.api.service.map](../index.md)/[WorldPointService](index.md)

# WorldPointService

[Kraken API]\
open class [WorldPointService](index.md)

A utility class for compressing and decompressing WorldPoint objects into and from integers.

## Constructors

| | |
|---|---|
| [WorldPointService](-world-point-service.md) | [Kraken API]<br>constructor() |

## Functions

| Name | Summary |
|---|---|
| [dx](dx.md) | [Kraken API]<br>open fun [dx](dx.md)(packed: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), offset: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)<br>Offsets the compressed WorldPoint by the given amounts in each dimension. |
| [dxy](dxy.md) | [Kraken API]<br>open fun [dxy](dxy.md)(packed: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), offsetX: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), offsetY: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)<br>Offsets the compressed WorldPoint by the given amounts in each dimension. |
| [dy](dy.md) | [Kraken API]<br>open fun [dy](dy.md)(packed: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), offset: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)<br>Offsets the compressed WorldPoint by the given amounts in each dimension. |
| [fromPacked](from-packed.md) | [Kraken API]<br>open fun [fromPacked](from-packed.md)(packed: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): WorldPoint<br>Decompresses a compressed WorldPoint integer back into a WorldPoint. |
| [getPackedPlane](get-packed-plane.md) | [Kraken API]<br>open fun [getPackedPlane](get-packed-plane.md)(packed: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Byte](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte/index.html)<br>Extracts the plane from a compressed WorldPoint integer. |
| [getPackedX](get-packed-x.md) | [Kraken API]<br>open fun [getPackedX](get-packed-x.md)(packed: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Short](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-short/index.html)<br>Extracts the X coordinate from a compressed WorldPoint integer. |
| [getPackedY](get-packed-y.md) | [Kraken API]<br>open fun [getPackedY](get-packed-y.md)(packed: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Short](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-short/index.html)<br>Extracts the Y coordinate from a compressed WorldPoint integer. |
| [pack](pack.md) | [Kraken API]<br>open fun [pack](pack.md)(wp: WorldPoint): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)<br>Compresses a WorldPoint into a single integer.<br>[Kraken API]<br>open fun [pack](pack.md)(x: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), y: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), z: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)<br>Compresses x, y, z coordinates into a single integer. |
| [unpack](unpack.md) | [Kraken API]<br>open fun [unpack](unpack.md)(packed: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): WorldPoint<br>Overloaded method wrapping `fromPacked`. |
