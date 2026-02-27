//[lib](../../../index.md)/[com.kraken.api.core.packet.entity](../index.md)/[MovementPackets](index.md)/[queueMovement](queue-movement.md)

# queueMovement

[Kraken API]\
open fun [queueMovement](queue-movement.md)(worldPointX: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), worldPointY: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), ctrlDown: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html))

Queues a movement packet for a specific world point x and y location.

#### Parameters

Kraken API

| | |
|---|---|
| worldPointX | The X location of the world point |
| worldPointY | The Y location of the world point |
| ctrlDown | True if control should be pressed (walks when run is toggled on and runs when walk is toggled on). |

[Kraken API]\
open fun [queueMovement](queue-movement.md)(location: WorldPoint)

Queues a movement packet for a specific world point.

#### Parameters

Kraken API

| | |
|---|---|
| location | The world point to queue a packet to move to. |

[Kraken API]\
open fun [queueMovement](queue-movement.md)(location: LocalPoint)

Queues a movement packet for a specific local point.

#### Parameters

Kraken API

| | |
|---|---|
| location | The local point to queue a packet to move to. |
