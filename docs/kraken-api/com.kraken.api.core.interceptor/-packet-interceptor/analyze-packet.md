//[kraken-api](../../../index.md)/[com.kraken.api.core.interceptor](../index.md)/[PacketInterceptor](index.md)/[analyzePacket](analyze-packet.md)

# analyzePacket

[Kraken API]\
open fun [analyzePacket](analyze-packet.md)(packetBufferNode: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)): [EncodedPacket](../../com.kraken.api.core.interceptor.model/-encoded-packet/index.md)

Extracts the encrypted Packet id (not opcode), size, and byte array payload from the PacketBufferNode.

#### Return

EncodedPacket The encoded packet, containing the id (not opcode), size, and byte array payload.

#### Parameters

Kraken API

| | |
|---|---|
| packetBufferNode | The packet buffer node object. |
