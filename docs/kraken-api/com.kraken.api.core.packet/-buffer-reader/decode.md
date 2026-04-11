//[kraken-api](../../../index.md)/[com.kraken.api.core.packet](../index.md)/[BufferReader](index.md)/[decode](decode.md)

# decode

[Kraken API]\
open fun [decode](decode.md)(payload: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[Byte](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte/index.html)&gt;, writes: [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;[PacketWrite](../../com.kraken.api.core.packet.model/-packet-write/index.md)&gt;): [Map](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/Map.html)&lt;[String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html), [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)&gt;

Decodes an encoded packet payload into a map of `param → value` entries, in the order the writes were originally performed.

#### Return

a [LinkedHashMap](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/LinkedHashMap.html) preserving write order, values as [Integer](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Integer.html) or [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)

#### Parameters

Kraken API

| | |
|---|---|
| payload | the raw bytes captured from the packet buffer (after Isaac cipher decoding) |
| writes | the ordered list of [PacketWrite](../../com.kraken.api.core.packet.model/-packet-write/index.md) descriptors for this packet opcode |
