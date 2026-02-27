//[lib](../../../index.md)/[com.kraken.api.input.mouse.strategy.replay](../index.md)/[PathLibrary](index.md)

# PathLibrary

[Kraken API]\
open class [PathLibrary](index.md)

## Constructors

| | |
|---|---|
| [PathLibrary](-path-library.md) | [Kraken API]<br>constructor() |

## Properties

| Name | Summary |
|---|---|
| [DATA_DIR](-d-a-t-a_-d-i-r.md) | [Kraken API]<br>val [DATA_DIR](-d-a-t-a_-d-i-r.md): [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html) |

## Functions

| Name | Summary |
|---|---|
| [getSimilarPath](get-similar-path.md) | [Kraken API]<br>open fun [getSimilarPath](get-similar-path.md)(candidates: [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;[NormalizedPath](../../com.kraken.api.input.mouse.model/-normalized-path/index.md)&gt;, targetDistance: [Double](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-double/index.html)): [NormalizedPath](../../com.kraken.api.input.mouse.model/-normalized-path/index.md)<br>Selects a [NormalizedPath](../../com.kraken.api.input.mouse.model/-normalized-path/index.md) from a provided list of candidate paths that most closely matches a target distance, considering tolerance thresholds and randomization within a subset of viable matches. |
| [load](load.md) | [Kraken API]<br>open fun [load](load.md)(library: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;[NormalizedPath](../../com.kraken.api.input.mouse.model/-normalized-path/index.md)&gt;<br>Loads and normalizes a set of mouse gestures from a specified library file, converting them into [NormalizedPath](../../com.kraken.api.input.mouse.model/-normalized-path/index.md) objects. |
