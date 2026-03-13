//[kraken-api](../../../index.md)/[com.kraken.api.input.mouse.strategy.bezier](../index.md)/[BezierStrategy](index.md)/[move](move.md)

# move

[Kraken API]\
open fun [move](move.md)(start: [Point](https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/java/awt/Point.html), target: [Point](https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/java/awt/Point.html))

Moves the mouse cursor along a cubic Bezier curve path to the specified target point. 

This method simulates a natural, human-like cursor movement by calculating and following a cubic Bezier curve between the mouse's current position and the target point. The curve includes two intermediate control points for smooth motion and incorporates random variations to add unpredictability. The motion speed and duration are dynamically calculated using Fitts's Law principles and easing functions.

- The motion begins at the mouse's current position (start).
- Two random control points (p1, p2) determine the curvature.
- The motion transitions smoothly to the target position using easing functions.

#### Parameters

Kraken API

| | |
|---|---|
| target | The target position to which the mouse cursor will move. This is represented as a `Point`. |
