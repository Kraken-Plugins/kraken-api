//[kraken-api](../../../index.md)/[com.kraken.api.core.packet](../index.md)/[BufferReader](index.md)/[debugDecode](debug-decode.md)

# debugDecode

[Kraken API]\
open fun [debugDecode](debug-decode.md)(packetName: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html), payload: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[Byte](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte/index.html)&gt;, writes: [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;[PacketWrite](../../com.kraken.api.core.packet.model/-packet-write/index.md)&gt;): [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)

Decodes the payload and returns a human-readable string, e.g.: 

```kotlin
  OPOBJ1 { objectId=41096, worldPointX=3221, worldPointY=3219, ctrlDown=0, subop=0 }

```

#### Return

a debug string

#### Parameters

Kraken API

| | |
|---|---|
| packetName | the display name of the packet opcode (e.g. &quot;OPOBJ1&quot;) |
| payload | the encoded payload bytes |
| writes | the ordered [PacketWrite](../../com.kraken.api.core.packet.model/-packet-write/index.md) descriptors for this opcode |
