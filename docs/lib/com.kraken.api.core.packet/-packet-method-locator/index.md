//[lib](../../../index.md)/[com.kraken.api.core.packet](../index.md)/[PacketMethodLocator](index.md)

# PacketMethodLocator

[Kraken API]\
open class [PacketMethodLocator](index.md)

A static utility class to find and cache the obfuscated packet-sending method (&quot;addNode&quot;) from the game client. 

 This class is intended to be run once at startup within a RuneLite context and is not intended to be run directly by plugins. It should be called and instantiated through the `Context` class.

## Constructors

| | |
|---|---|
| [PacketMethodLocator](-packet-method-locator.md) | [Kraken API]<br>constructor() |

## Properties

| Name | Summary |
|---|---|
| [packetMethods](packet-methods.md) | [Kraken API]<br>open var [packetMethods](packet-methods.md): [PacketMethods](../../com.kraken.api.core.packet.model/-packet-methods/index.md) |

## Functions

| Name | Summary |
|---|---|
| [initialize](initialize.md) | [Kraken API]<br>open fun [initialize](initialize.md)(client: Client)<br>Initializes the packet method locator. |
