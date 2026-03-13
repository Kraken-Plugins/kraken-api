//[kraken-api](../../../index.md)/[com.kraken.api.input.mouse](../index.md)/[VirtualMouse](index.md)/[setMouseMovementStrategy](set-mouse-movement-strategy.md)

# setMouseMovementStrategy

[Kraken API]\
open fun [setMouseMovementStrategy](set-mouse-movement-strategy.md)(strategy: [MouseMovementStrategy](../../com.kraken.api.input.mouse.strategy/-mouse-movement-strategy/index.md))

Sets the strategy to be used for mouse movement in the system. 

 This method updates the default mouse movement behavior by replacing it with the provided @MouseMovementStrategy implementation. The new strategy will dictate how mouse movements are handled globally. 

**Note:** It is the caller's responsibility to ensure that the provided strategy implementation is valid and behaves as expected. Passing a `null` value may lead to unexpected behavior.

#### Parameters

Kraken API

| | |
|---|---|
| strategy | The @MouseMovementStrategy to be set as the default mouse movement strategy. Must not be `null`. |
