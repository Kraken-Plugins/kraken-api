//[kraken-api](../../../index.md)/[com.kraken.api.core.packet](../index.md)/[PacketClient](index.md)

# PacketClient

[Kraken API]\
open class [PacketClient](index.md)

`PacketClient` is an instance-based RuneLite client packet sending utility which uses reflection to construct and send low-level packets directly to the game servers. Generally, you should not need to use this class directly within your plugins as it functions at a lower level to construct and sending packets. 

 Instead, it's recommended to use the higher level API's like `MousePackets`, `WidgetPackets`, or `NpcPackets` for sending game packets to the server based on your specific entity interaction needs (clicking interfaces, NPC's, GameObjects, etc...

## Constructors

| | |
|---|---|
| [PacketClient](-packet-client.md) | [Kraken API]<br>constructor(client: Client)<br>Creates a new PacketSender. |

## Functions

| Name | Summary |
|---|---|
| [sendPacket](send-packet.md) | [Kraken API]<br>open fun [sendPacket](send-packet.md)(def: [PacketDefinition](../../com.kraken.api.core.packet.model/-packet-definition/index.md), objects: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)&gt;)<br>Constructs and sends a packet to the game server. |
