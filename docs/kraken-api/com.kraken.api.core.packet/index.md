//[kraken-api](../../index.md)/[com.kraken.api.core.packet](index.md)

# Package-level declarations

## Types

| Name | Summary |
|---|---|
| [BufferUtils](-buffer-utils/index.md) | [Kraken API]<br>open class [BufferUtils](-buffer-utils/index.md)<br>A static utility class that uses reflection to interact with the client's obfuscated buffer objects (e.g., PacketBuffer). |
| [ObfuscatedNames](-obfuscated-names/index.md) | [Kraken API]<br>class [ObfuscatedNames](-obfuscated-names/index.md)<br>This class is a direct copy of the ObfuscatedNames class from the EthanVann PacketUtils class found here: https://github.com/Ethan-Vann/PacketUtils/blob/master/src/main/java/com/example/PacketUtils/ObfuscatedNames.java The copy was made so that this API could remain fundamentally compatible with the popular EthanVann PacketUtils plugin. |
| [PacketBufferReader](-packet-buffer-reader/index.md) | [Kraken API]<br>open class [PacketBufferReader](-packet-buffer-reader/index.md) |
| [PacketClient](-packet-client/index.md) | [Kraken API]<br>open class [PacketClient](-packet-client/index.md)<br>`PacketClient` is an instance-based RuneLite client packet sending utility which uses reflection to construct and send low level packets directly to the game servers. |
| [PacketMethodLocator](-packet-method-locator/index.md) | [Kraken API]<br>open class [PacketMethodLocator](-packet-method-locator/index.md)<br>A static utility class to find and cache the obfuscated packet-sending method (&quot;addNode&quot;) from the game client. |
