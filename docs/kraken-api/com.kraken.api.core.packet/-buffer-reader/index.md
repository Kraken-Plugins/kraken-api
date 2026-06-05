//[kraken-api](../../../index.md)/[com.kraken.api.core.packet](../index.md)/[BufferReader](index.md)

# BufferReader

[Kraken API]\
open class [BufferReader](index.md)

Reverses the obfuscated buffer encoding written by [BufferUtils](../-buffer-utils/index.md). 

The write side encodes each field by: 

1. Advancing a logical `offset` by `offsetMultiplier` once per byte written.
2. Computing the real array index as `offset * indexMultiplier - 1`.
3. Writing one byte derived from the full field value via the [BufferOperation](../../com.kraken.api.core.packet.model/-buffer-operation/index.md): 
   
   - RAW → `(byte) value`
   - ADD(x) → `(byte)(x + value)`
   - SUBTRACT(x) → `(byte)(x - value)`
   - RIGHT_SHIFT(n) → `(byte)(value >> n)`

Reading back is the mirror image: 

- Replay the same offset arithmetic to find each byte in the payload.
- Invert the obfuscation to recover the byte's bit contribution.
- OR all bit contributions for the same field together to get the original value.

The `indexMultiplier` and `offsetMultiplier` are the same obfuscated constants used by [BufferUtils](../-buffer-utils/index.md) and are loaded from PacketFactory.getReflectionHooks.

## Constructors

| | |
|---|---|
| [BufferReader](-buffer-reader.md) | [Kraken API]<br>constructor() |

## Functions

| Name | Summary |
|---|---|
| [debugDecode](debug-decode.md) | [Kraken API]<br>open fun [debugDecode](debug-decode.md)(packetName: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html), payload: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[Byte](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte/index.html)&gt;, writes: [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;[PacketWrite](../../com.kraken.api.core.packet.model/-packet-write/index.md)&gt;): [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)<br>Decodes the payload and returns a human-readable string, e.g. |
| [decode](decode.md) | [Kraken API]<br>open fun [decode](decode.md)(payload: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[Byte](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-byte/index.html)&gt;, writes: [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;[PacketWrite](../../com.kraken.api.core.packet.model/-packet-write/index.md)&gt;): [Map](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/Map.html)&lt;[String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html), [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)&gt;<br>Decodes an encoded packet payload into a map of `param → value` entries, in the order the writes were originally performed. |
