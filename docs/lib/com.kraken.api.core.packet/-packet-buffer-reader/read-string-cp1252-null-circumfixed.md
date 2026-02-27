//[lib](../../../index.md)/[com.kraken.api.core.packet](../index.md)/[PacketBufferReader](index.md)/[readStringCp1252NullCircumfixed](read-string-cp1252-null-circumfixed.md)

# readStringCp1252NullCircumfixed

[Kraken API]\
open fun [readStringCp1252NullCircumfixed](read-string-cp1252-null-circumfixed.md)(data: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[Byte](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte/index.html)&gt;, startPos: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)

Reads a CP1252 null-circumfixed string from the byte array. Format: [0x00][string bytes][0x00]

#### Return

The decoded string.

#### Parameters

Kraken API

| | |
|---|---|
| data | The byte array. |
| startPos | The starting position (should be at the leading null byte). |
