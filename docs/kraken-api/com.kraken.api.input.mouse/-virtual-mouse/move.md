//[kraken-api](../../../index.md)/[com.kraken.api.input.mouse](../index.md)/[VirtualMouse](index.md)/[move](move.md)

# move

[Kraken API]\
open fun [move](move.md)(target: [Point](https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/java/awt/Point.html), strategy: [MouseMovementStrategy](../../com.kraken.api.input.mouse.strategy/-mouse-movement-strategy/index.md)): [VirtualMouse](index.md)

Moves the mouse to the specified target position using the provided movement strategy. 

This method uses the given `MouseMovementStrategy` to dictate how the mouse moves to the specified `Point` target. The movement behavior varies depending on the selected strategy (e.g., linear, bezier, instant, etc.).

- If the movement strategy is not properly initialized, unexpected behavior may occur.
- The mouse's final position will be the given `Point` after the strategy completes.

#### Return

The [VirtualMouse](index.md) instance for method chaining.

#### Parameters

Kraken API

| | |
|---|---|
| target | The target position represented as a [Point](https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/java/awt/Point.html) to which the mouse will be moved. |
| strategy | The [MouseMovementStrategy](../../com.kraken.api.input.mouse.strategy/-mouse-movement-strategy/index.md) defining how the mouse moves to the target point. |

[Kraken API]\
open fun [move](move.md)(target: [Point](https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/java/awt/Point.html), config: [WindMouseConfig](../../com.kraken.api.input.mouse.strategy.wind/-wind-mouse-config/index.md)): [VirtualMouse](index.md)

Moves the mouse using the wind mouse movement strategy using a custom set of wind mouse configuration.

#### Return

The [VirtualMouse](index.md) instance for method chaining.

#### Parameters

Kraken API

| | |
|---|---|
| target | The target point for the mouse |
| config | Custom wind mouse configuration object |

[Kraken API]\
open fun [move](move.md)(target: [Point](https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/java/awt/Point.html)): [VirtualMouse](index.md)

Moves the mouse to the specified target position using the default mouse movement strategy. 

This method leverages the default `MouseMovementStrategy` to determine how the mouse moves. The behavior of the movement may include various strategies (e.g., linear, curved, instant, etc.) depending on the configuration of the default strategy.

- If the default mouse movement strategy is not initialized, the behavior may be undefined.
- The mouse's final position will be the specified target after the strategy completes its execution.

#### Return

The [VirtualMouse](index.md) instance for method chaining.

#### Parameters

Kraken API

| | |
|---|---|
| target | The target position represented as a [Point](https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/java/awt/Point.html) to which the mouse will move. |

[Kraken API]\
open fun [move](move.md)(actor: Actor): [VirtualMouse](index.md)

Moves the mouse to the specified Actor.

#### Return

The VirtualMouse instance for chaining.

#### Parameters

Kraken API

| | |
|---|---|
| actor | The actor to move to. |

[Kraken API]\
open fun [move](move.md)(actor: Actor, mouseMovementStrategy: [MouseMovementStrategy](../../com.kraken.api.input.mouse.strategy/-mouse-movement-strategy/index.md)): [VirtualMouse](index.md)

Moves the mouse to the specified Actor using the provided movement strategy.

#### Return

The VirtualMouse instance for chaining.

#### Parameters

Kraken API

| | |
|---|---|
| actor | The actor to move to. |
| mouseMovementStrategy | The movement strategy to use. |

[Kraken API]\
open fun [move](move.md)(item: [ContainerItem](../../com.kraken.api.query.container/-container-item/index.md)): [VirtualMouse](index.md)

Moves the mouse to the specified ContainerItem.

#### Return

The VirtualMouse instance for chaining.

#### Parameters

Kraken API

| | |
|---|---|
| item | The container item to move to. |

[Kraken API]\
open fun [move](move.md)(item: [ContainerItem](../../com.kraken.api.query.container/-container-item/index.md), mouseMovementStrategy: [MouseMovementStrategy](../../com.kraken.api.input.mouse.strategy/-mouse-movement-strategy/index.md)): [VirtualMouse](index.md)

Moves the mouse to the specified ContainerItem using the provided movement strategy.

#### Return

The VirtualMouse instance for chaining.

#### Parameters

Kraken API

| | |
|---|---|
| item | The container item to move to. |
| mouseMovementStrategy | The movement strategy to use. |

[Kraken API]\
open fun [move](move.md)(tileObject: TileObject): [VirtualMouse](index.md)

Moves the mouse to the specified TileObject.

#### Return

The VirtualMouse instance for chaining.

#### Parameters

Kraken API

| | |
|---|---|
| tileObject | The tile object to move to. |

[Kraken API]\
open fun [move](move.md)(tileObject: TileObject, mouseMovementStrategy: [MouseMovementStrategy](../../com.kraken.api.input.mouse.strategy/-mouse-movement-strategy/index.md)): [VirtualMouse](index.md)

Moves the mouse to the specified TileObject using the provided movement strategy.

#### Return

The VirtualMouse instance for chaining.

#### Parameters

Kraken API

| | |
|---|---|
| tileObject | The tile object to move to. |
| mouseMovementStrategy | The movement strategy to use. |

[Kraken API]\
open fun [move](move.md)(tile: Tile): [VirtualMouse](index.md)

Moves the mouse to the specified Tile.

#### Return

The VirtualMouse instance for chaining.

#### Parameters

Kraken API

| | |
|---|---|
| tile | The tile to move to. |

[Kraken API]\
open fun [move](move.md)(tile: Tile, mouseMovementStrategy: [MouseMovementStrategy](../../com.kraken.api.input.mouse.strategy/-mouse-movement-strategy/index.md)): [VirtualMouse](index.md)

Moves the mouse to the specified Tile using the provided movement strategy.

#### Return

The VirtualMouse instance for chaining.

#### Parameters

Kraken API

| | |
|---|---|
| tile | The tile to move to. |
| mouseMovementStrategy | The movement strategy to use. |

[Kraken API]\
open fun [move](move.md)(widget: Widget): [VirtualMouse](index.md)

Moves the mouse to the specified Widget.

#### Return

The VirtualMouse instance for chaining.

#### Parameters

Kraken API

| | |
|---|---|
| widget | The widget to move to. |

[Kraken API]\
open fun [move](move.md)(widget: Widget, mouseMovementStrategy: [MouseMovementStrategy](../../com.kraken.api.input.mouse.strategy/-mouse-movement-strategy/index.md)): [VirtualMouse](index.md)

Moves the mouse to the specified Widget using the provided movement strategy.

#### Return

The VirtualMouse instance for chaining.

#### Parameters

Kraken API

| | |
|---|---|
| widget | The widget to move to. |
| mouseMovementStrategy | The movement strategy to use. |

[Kraken API]\
open fun [move](move.md)(worldPoint: WorldPoint): [VirtualMouse](index.md)

Moves the mouse to the specified WorldPoint.

#### Return

The VirtualMouse instance for chaining.

#### Parameters

Kraken API

| | |
|---|---|
| worldPoint | The world point to move to. |

[Kraken API]\
open fun [move](move.md)(worldPoint: WorldPoint, mouseMovementStrategy: [MouseMovementStrategy](../../com.kraken.api.input.mouse.strategy/-mouse-movement-strategy/index.md)): [VirtualMouse](index.md)

Moves the mouse to the specified WorldPoint using the provided movement strategy.

#### Return

The VirtualMouse instance for chaining.

#### Parameters

Kraken API

| | |
|---|---|
| worldPoint | The world point to move to. |
| mouseMovementStrategy | The movement strategy to use. |

[Kraken API]\
open fun [move](move.md)(localPoint: LocalPoint): [VirtualMouse](index.md)

Moves the mouse to the specified LocalPoint.

#### Return

The VirtualMouse instance for chaining.

#### Parameters

Kraken API

| | |
|---|---|
| localPoint | The local point to move to. |

[Kraken API]\
open fun [move](move.md)(localPoint: LocalPoint, mouseMovementStrategy: [MouseMovementStrategy](../../com.kraken.api.input.mouse.strategy/-mouse-movement-strategy/index.md)): [VirtualMouse](index.md)

Moves the mouse to the specified LocalPoint using the provided movement strategy.

#### Return

The VirtualMouse instance for chaining.

#### Parameters

Kraken API

| | |
|---|---|
| localPoint | The local point to move to. |
| mouseMovementStrategy | The movement strategy to use. |

[Kraken API]\
open fun [move](move.md)(localPoint: LocalPoint, plane: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [VirtualMouse](index.md)

Moves the mouse to the specified LocalPoint at a specific plane.

#### Return

The VirtualMouse instance for chaining.

#### Parameters

Kraken API

| | |
|---|---|
| localPoint | The local point to move to. |
| plane | The plane to move to. |

[Kraken API]\
open fun [move](move.md)(localPoint: LocalPoint, plane: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), mouseMovementStrategy: [MouseMovementStrategy](../../com.kraken.api.input.mouse.strategy/-mouse-movement-strategy/index.md)): [VirtualMouse](index.md)

Moves the mouse to the specified LocalPoint at a specific plane using the provided movement strategy.

#### Return

The VirtualMouse instance for chaining.

#### Parameters

Kraken API

| | |
|---|---|
| localPoint | The local point to move to. |
| plane | The plane to move to. |
| mouseMovementStrategy | The movement strategy to use. |

[Kraken API]\
open fun [move](move.md)(rect: [Rectangle](https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/java/awt/Rectangle.html)): [VirtualMouse](index.md)

Moves the mouse to the center of the specified rectangle.

#### Return

The VirtualMouse instance for chaining.

#### Parameters

Kraken API

| | |
|---|---|
| rect | The rectangle to move to. |

[Kraken API]\
open fun [move](move.md)(polygon: [Polygon](https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/java/awt/Polygon.html)): [VirtualMouse](index.md)

Moves the mouse to the center of the specified polygon.

#### Return

The VirtualMouse instance for chaining.

#### Parameters

Kraken API

| | |
|---|---|
| polygon | The polygon to move to. |
