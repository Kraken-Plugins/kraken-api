//[lib](../../../index.md)/[com.kraken.api.core.packet](../index.md)/[BufferUtils](index.md)/[encodeStringCp1252](encode-string-cp1252.md)

# encodeStringCp1252

[Kraken API]\
open fun [encodeStringCp1252](encode-string-cp1252.md)(data: [CharSequence](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/CharSequence.html), startIndex: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), endIndex: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), output: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[Byte](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte/index.html)&gt;, outputStartIndex: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)

Encodes a Java String (CharSequence) into a byte array using the CP1252 character set. This is the standard string encoding used by OSRS, which includes special characters like €, ‚, ƒ, „, etc., that are not in standard ASCII.

#### Return

The total number of bytes written.

#### Parameters

Kraken API

| | |
|---|---|
| data | The string data to encode. |
| startIndex | The starting character index from the string. |
| endIndex | The ending character index from the string. |
| output | The destination byte array. |
| outputStartIndex | The starting index in the byte array to write to. |
