//[kraken-api](../../../index.md)/[com.kraken.api.core.packet.entity](../index.md)/[MousePackets](index.md)/[queueClickPacket](queue-click-packet.md)

# queueClickPacket

[Kraken API]\
open fun [queueClickPacket](queue-click-packet.md)(x: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), y: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html))

Queues a click packet to send to the game server. The click packet should be sent before any game interaction (Widget, Movement, Npc, Object etc...) packets are sent. The click packet encapsulates the x and y coordinates of the canvas for the click that was made.

#### Parameters

Kraken API

| | |
|---|---|
| x | The x canvas coordinate. |
| y | The y canvas coordinate. |
