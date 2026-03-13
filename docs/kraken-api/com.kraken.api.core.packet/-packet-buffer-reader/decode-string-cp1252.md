//[kraken-api](../../../index.md)/[com.kraken.api.core.packet](../index.md)/[PacketBufferReader](index.md)/[decodeStringCp1252](decode-string-cp1252.md)

# decodeStringCp1252

[Kraken API]\
open fun [decodeStringCp1252](decode-string-cp1252.md)(data: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[Byte](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte/index.html)&gt;, startIndex: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), endIndex: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)

Decodes a byte array range from CP1252 encoding back to a Java String. This reverses the encodeStringCp1252 method.

#### Return

The decoded string.

#### Parameters

Kraken API

| | |
|---|---|
| data | The byte array containing CP1252 encoded data. |
| startIndex | The starting index in the array. |
| endIndex | The ending index (exclusive). |
