//[kraken-api](../../../index.md)/[com.kraken.api.util](../index.md)/[StringUtils](index.md)/[stripColTags](strip-col-tags.md)

# stripColTags

[Kraken API]\
open fun [stripColTags](strip-col-tags.md)(sourceList: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)&gt;): [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)&gt;

Strips `<col=...>` tags from each string in the provided array.

#### Return

a new array with `<col=...>` tags removed

#### Parameters

Kraken API

| | |
|---|---|
| sourceList | the array of strings to process |

[Kraken API]\
open fun [stripColTags](strip-col-tags.md)(source: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)): [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)

Strips `<col=...>` tags from a single string.

#### Return

the string with all color tags removed

#### Parameters

Kraken API

| | |
|---|---|
| source | the string to process |
