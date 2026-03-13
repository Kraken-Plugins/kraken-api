//[kraken-api](../../../index.md)/[com.kraken.api.core.packet](../index.md)/[PacketBufferReader](index.md)/[readInt](read-int.md)

# readInt

[Kraken API]\
open fun [readInt](read-int.md)(data: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[Byte](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte/index.html)&gt;, pos: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)

Reads an integer from the byte array at the given position.

#### Return

The 32-bit integer value, or 0 if the position is out of bounds.

#### Parameters

Kraken API

| | |
|---|---|
| data | The byte array. |
| pos | The starting index (expects 4 bytes available). |
