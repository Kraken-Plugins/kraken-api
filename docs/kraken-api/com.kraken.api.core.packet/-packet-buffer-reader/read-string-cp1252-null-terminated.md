//[kraken-api](../../../index.md)/[com.kraken.api.core.packet](../index.md)/[PacketBufferReader](index.md)/[readStringCp1252NullTerminated](read-string-cp1252-null-terminated.md)

# readStringCp1252NullTerminated

[Kraken API]\
open fun [readStringCp1252NullTerminated](read-string-cp1252-null-terminated.md)(data: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[Byte](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte/index.html)&gt;, startPos: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)

Reads a CP1252 null-terminated string from the byte array. Format: [string bytes][0x00]

#### Return

The decoded string.

#### Parameters

Kraken API

| | |
|---|---|
| data | The byte array. |
| startPos | The starting position in the array. |
