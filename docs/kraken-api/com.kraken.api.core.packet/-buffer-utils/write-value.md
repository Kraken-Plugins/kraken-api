//[kraken-api](../../../index.md)/[com.kraken.api.core.packet](../index.md)/[BufferUtils](index.md)/[writeValue](write-value.md)

# writeValue

[Kraken API]\
open fun [writeValue](write-value.md)(writeDescription: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html), value: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), bufferInstance: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html))

Writes a single integer value to the buffer using a specified &quot;write description&quot;. This method handles the client's obfuscated write types (add, subtract, etc.) and the complex index calculation.

#### Parameters

Kraken API

| | |
|---|---|
| writeDescription | A string (e.g., &quot;a128&quot;, &quot;s&quot;, &quot;v&quot;) defining the write operation. |
| value | The integer value to write. |
| bufferInstance | The obfuscated buffer object. |
