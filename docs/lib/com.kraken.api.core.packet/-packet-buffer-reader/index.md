//[lib](../../../index.md)/[com.kraken.api.core.packet](../index.md)/[PacketBufferReader](index.md)

# PacketBufferReader

[Kraken API]\
open class [PacketBufferReader](index.md)

## Constructors

| | |
|---|---|
| [PacketBufferReader](-packet-buffer-reader.md) | [Kraken API]<br>constructor() |

## Functions

| Name | Summary |
|---|---|
| [decodeStringCp1252](decode-string-cp1252.md) | [Kraken API]<br>open fun [decodeStringCp1252](decode-string-cp1252.md)(data: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[Byte](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte/index.html)&gt;, startIndex: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), endIndex: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)<br>Decodes a byte array range from CP1252 encoding back to a Java String. |
| [readByte](read-byte.md) | [Kraken API]<br>open fun [readByte](read-byte.md)(data: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[Byte](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte/index.html)&gt;, pos: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)<br>Reads a single byte from the array at the given position. |
| [readInt](read-int.md) | [Kraken API]<br>open fun [readInt](read-int.md)(data: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[Byte](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte/index.html)&gt;, pos: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)<br>Reads an integer from the byte array at the given position. |
| [readPacketBuffer](read-packet-buffer.md) | [Kraken API]<br>open fun [readPacketBuffer](read-packet-buffer.md)(packetBufferNode: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)): [PacketData](../../com.kraken.api.core.packet.model/-packet-data/index.md)<br>Reads the raw byte data from a packet buffer node using reflection. |
| [readShort](read-short.md) | [Kraken API]<br>open fun [readShort](read-short.md)(data: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[Byte](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte/index.html)&gt;, pos: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)<br>Reads a short from the byte array at the given position. |
| [readStringCp1252NullCircumfixed](read-string-cp1252-null-circumfixed.md) | [Kraken API]<br>open fun [readStringCp1252NullCircumfixed](read-string-cp1252-null-circumfixed.md)(data: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[Byte](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte/index.html)&gt;, startPos: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)<br>Reads a CP1252 null-circumfixed string from the byte array. |
| [readStringCp1252NullTerminated](read-string-cp1252-null-terminated.md) | [Kraken API]<br>open fun [readStringCp1252NullTerminated](read-string-cp1252-null-terminated.md)(data: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[Byte](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte/index.html)&gt;, startPos: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)<br>Reads a CP1252 null-terminated string from the byte array. |
| [toHexString](to-hex-string.md) | [Kraken API]<br>open fun [toHexString](to-hex-string.md)(bytes: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[Byte](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte/index.html)&gt;): [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)<br>Converts byte array to hex string for display. |
