//[lib](../../../index.md)/[com.kraken.api.core.packet](../index.md)/[PacketBufferReader](index.md)/[readShort](read-short.md)

# readShort

[Kraken API]\
open fun [readShort](read-short.md)(data: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[Byte](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte/index.html)&gt;, pos: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)

Reads a short from the byte array at the given position.

#### Return

The 16-bit value as an integer, or 0 if out of bounds.

#### Parameters

Kraken API

| | |
|---|---|
| data | The byte array. |
| pos | The starting index (expects 2 bytes available). |
