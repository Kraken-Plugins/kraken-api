//[kraken-api](../../../index.md)/[com.kraken.api.util](../index.md)/[StringUtils](index.md)

# StringUtils

[Kraken API]\
open class [StringUtils](index.md)

## Constructors

| | |
|---|---|
| [StringUtils](-string-utils.md) | [Kraken API]<br>constructor() |

## Functions

| Name | Summary |
|---|---|
| [addColTags](add-col-tags.md) | [Kraken API]<br>open fun [addColTags](add-col-tags.md)(text: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)): [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)<br>Wraps the provided text in a standard color tag `<col=ff9040>`. |
| [decrypt](decrypt.md) | [Kraken API]<br>open fun [decrypt](decrypt.md)(base64IvAndCiphertext: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html), key: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)): [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)<br>Decrypts a Base64 encoded string containing an IV and ciphertext. |
| [encrypt](encrypt.md) | [Kraken API]<br>open fun [encrypt](encrypt.md)(plaintext: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html), key: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)): [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)<br>Encrypts the given plaintext using AES/CBC/PKCS5Padding. |
| [getIndex](get-index.md) | [Kraken API]<br>open fun [getIndex](get-index.md)(terms: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)&gt;, term: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)<br>Finds the index of a term in an array of terms, ignoring case. |
| [stripColTags](strip-col-tags.md) | [Kraken API]<br>open fun [stripColTags](strip-col-tags.md)(source: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)): [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)<br>Strips `<col=...>` tags from a single string.<br>[Kraken API]<br>open fun [stripColTags](strip-col-tags.md)(sourceList: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)&gt;): [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)&gt;<br>Strips `<col=...>` tags from each string in the provided array. |
