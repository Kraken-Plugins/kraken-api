//[kraken-api](../../../index.md)/[com.kraken.api.input.mouse.strategy.replay](../index.md)/[MotionFactory](index.md)

# MotionFactory

[Kraken API]\
open class [MotionFactory](index.md)

## Constructors

| | |
|---|---|
| [MotionFactory](-motion-factory.md) | [Kraken API]<br>constructor() |

## Types

| Name | Summary |
|---|---|
| [TimedPoint](-timed-point/index.md) | [Kraken API]<br>open class [TimedPoint](-timed-point/index.md) |

## Functions

| Name | Summary |
|---|---|
| [transform](transform.md) | [Kraken API]<br>open fun [transform](transform.md)(template: [NormalizedPath](../../com.kraken.api.input.mouse.model/-normalized-path/index.md), start: Point, end: Point, duration: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html)): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;[MotionFactory.TimedPoint](-timed-point/index.md)&gt;<br>Transforms a normalized path into a real-world path by scaling, rotating, translating, and timing its points based on the given start and end coordinates and the duration of the motion. |
