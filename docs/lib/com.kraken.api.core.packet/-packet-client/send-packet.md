//[lib](../../../index.md)/[com.kraken.api.core.packet](../index.md)/[PacketClient](index.md)/[sendPacket](send-packet.md)

# sendPacket

[Kraken API]\
open fun [sendPacket](send-packet.md)(def: [PacketDefinition](../../com.kraken.api.core.packet.model/-packet-definition/index.md), objects: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)&gt;)

Constructs and sends a packet to the game server. This is the primary public method of this class.

#### Parameters

Kraken API

| | |
|---|---|
| def | The [PacketDefinition](../../com.kraken.api.core.packet.model/-packet-definition/index.md) enumeration defining the packet structure. |
| objects | The data (payload) for the packet, in the order defined by the PacketDefinition. |
