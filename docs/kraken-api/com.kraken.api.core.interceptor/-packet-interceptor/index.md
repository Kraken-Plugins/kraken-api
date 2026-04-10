//[kraken-api](../../../index.md)/[com.kraken.api.core.interceptor](../index.md)/[PacketInterceptor](index.md)

# PacketInterceptor

[Kraken API]\
open class [PacketInterceptor](index.md)

## Constructors

| | |
|---|---|
| [PacketInterceptor](-packet-interceptor.md) | [Kraken API]<br>constructor(client: Client) |

## Types

| Name | Summary |
|---|---|
| [PacketHookAdvice](-packet-hook-advice/index.md) | [Kraken API]<br>open class [PacketHookAdvice](-packet-hook-advice/index.md)<br>The Advice class injected directly into &quot;addNode&quot;. |

## Properties

| Name | Summary |
|---|---|
| [client](client.md) | [Kraken API]<br>open var [client](client.md): Client |
| [eventBus](event-bus.md) | [Kraken API]<br>val [eventBus](event-bus.md): EventBus |
| [injected](injected.md) | [Kraken API]<br>open var [injected](injected.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [instance](instance.md) | [Kraken API]<br>open var [instance](instance.md): [PacketInterceptor](index.md) |

## Functions

| Name | Summary |
|---|---|
| [analyzePacket](analyze-packet.md) | [Kraken API]<br>open fun [analyzePacket](analyze-packet.md)(packetBufferNode: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)): [EncodedPacket](../../com.kraken.api.core.interceptor.model/-encoded-packet/index.md)<br>Extracts the encrypted Packet id (not opcode), size, and byte array payload from the PacketBufferNode. |
| [injectHook](inject-hook.md) | [Kraken API]<br>open fun [injectHook](inject-hook.md)()<br>Modifies the bytecode of the &quot;addNode&quot; method within the client at runtime to invoke the [PacketHookAdvice](-packet-hook-advice/index.md) class whenever the method is called. |
