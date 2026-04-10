//[kraken-api](../../../index.md)/[com.kraken.api.core.packet](../index.md)/[BufferUtils](index.md)

# BufferUtils

[Kraken API]\
open class [BufferUtils](index.md)

A static utility class that uses reflection to interact with the client's obfuscated buffer objects (e.g., PacketBuffer). This class provides a stable API to get/set the buffer's underlying byte array and its current offset, and to write data using the client's specific (and obfuscated) methods.

## Constructors

| | |
|---|---|
| [BufferUtils](-buffer-utils.md) | [Kraken API]<br>constructor() |

## Functions

| Name | Summary |
|---|---|
| [encodeStringCp1252](encode-string-cp1252.md) | [Kraken API]<br>open fun [encodeStringCp1252](encode-string-cp1252.md)(data: [CharSequence](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/CharSequence.html), startIndex: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), endIndex: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), output: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[Byte](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte/index.html)&gt;, outputStartIndex: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)<br>Encodes a Java String (CharSequence) into a byte array using the CP1252 character set. |
| [getArray](get-array.md) | [Kraken API]<br>open fun [getArray](get-array.md)(bufferInstance: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)): [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[Byte](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte/index.html)&gt;<br>Reflectively gets the underlying 'array' (the byte[]) from a buffer instance. |
| [getOffset](get-offset.md) | [Kraken API]<br>open fun [getOffset](get-offset.md)(bufferInstance: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)<br>Reflectively gets the 'offset' (current write position) from a buffer instance. |
| [nextIndex](next-index.md) | [Kraken API]<br>open fun [nextIndex](next-index.md)(offset: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)<br>Calculates the *next logical offset* by adding the obfuscated offset multiplier. |
| [setArray](set-array.md) | [Kraken API]<br>open fun [setArray](set-array.md)(bufferInstance: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html), array: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[Byte](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte/index.html)&gt;)<br>Reflectively sets the underlying 'array' (the byte[]) on a buffer instance. |
| [setOffset](set-offset.md) | [Kraken API]<br>open fun [setOffset](set-offset.md)(bufferInstance: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html), offset: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html))<br>Reflectively sets the 'offset' (current write position) on a buffer instance. |
| [writeOperation](write-operation.md) | [Kraken API]<br>open fun [writeOperation](write-operation.md)(operation: [BufferOperation](../../com.kraken.api.core.packet.model/-buffer-operation/index.md), value: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html), bufferInstance: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html))<br>Writes a single typed buffer operation to the packet buffer. |
| [writeStringCp1252NullCircumfixed](write-string-cp1252-null-circumfixed.md) | [Kraken API]<br>open fun [writeStringCp1252NullCircumfixed](write-string-cp1252-null-circumfixed.md)(val: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html), bufferInstance: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html))<br>Writes a string to the buffer, encoded in CP1252, with a null (0) byte *before* and *after* the string. |
| [writeStringCp1252NullTerminated](write-string-cp1252-null-terminated.md) | [Kraken API]<br>open fun [writeStringCp1252NullTerminated](write-string-cp1252-null-terminated.md)(val: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html), bufferInstance: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html))<br>Writes a string to the buffer, encoded in CP1252, followed by a single null (0) byte terminator. |
