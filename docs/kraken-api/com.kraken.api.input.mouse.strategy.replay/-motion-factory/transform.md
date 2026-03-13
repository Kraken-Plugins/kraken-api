//[kraken-api](../../../index.md)/[com.kraken.api.input.mouse.strategy.replay](../index.md)/[MotionFactory](index.md)/[transform](transform.md)

# transform

[Kraken API]\
open fun [transform](transform.md)(template: [NormalizedPath](../../com.kraken.api.input.mouse.model/-normalized-path/index.md), start: Point, end: Point, duration: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html)): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;[MotionFactory.TimedPoint](-timed-point/index.md)&gt;

Transforms a normalized path into a real-world path by scaling, rotating, translating, and timing its points based on the given start and end coordinates and the duration of the motion. 

 The transformation is achieved in the following steps: 

- Scaling: Scales the normalized coordinates to match the target distance between the start and end points.
- Rotation: Rotates the points to align with the angle between the start and end points.
- Translation: Translates the points to the starting position.
- Timing: Maps the normalized time of each point to the provided duration.

#### Return

A List&lt;TimedPoint&gt; where each TimedPoint represents a point on the real-world motion path with x, y coordinates and an associated timestamp.

#### Parameters

Kraken API

| | |
|---|---|
| template | The @link NormalizedPath containing the normalized unit points to be transformed. It defines the shape and time distribution of the motion path. |
| start | The @link Point specifying the starting position of the motion. |
| end | The @link Point specifying the ending position of the motion. |
| duration | A long value representing the total duration of the motion in milliseconds. |
