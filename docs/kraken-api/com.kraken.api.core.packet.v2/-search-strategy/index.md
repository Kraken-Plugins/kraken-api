//[kraken-api](../../../index.md)/[com.kraken.api.core.packet.v2](../index.md)/[SearchStrategy](index.md)

# SearchStrategy

[Kraken API]\
enum [SearchStrategy](index.md)

## Entries

| | |
|---|---|
| [BY_NAME](-b-y_-n-a-m-e/index.md) | [Kraken API]<br>[BY_NAME](-b-y_-n-a-m-e/index.md)<br>Default. Match entries where the non-obfuscated &quot;name&quot; field equals searchName. |
| [BY_DESCRIPTOR_TYPE](-b-y_-d-e-s-c-r-i-p-t-o-r_-t-y-p-e/index.md) | [Kraken API]<br>[BY_DESCRIPTOR_TYPE](-b-y_-d-e-s-c-r-i-p-t-o-r_-t-y-p-e/index.md)<br>Match fields whose descriptor references a class by its non-obfuscated name. searchName should be the non-obfuscated class name (e.g. &quot;IsaacCipher&quot;). The resolver will look up that class's obfuscated name at runtime, then scan for fields with descriptor &quot;L{obfuscatedName};&quot;. This avoids any hardcoded obfuscated values. |
| [BY_DESCRIPTOR](-b-y_-d-e-s-c-r-i-p-t-o-r/index.md) | [Kraken API]<br>[BY_DESCRIPTOR](-b-y_-d-e-s-c-r-i-p-t-o-r/index.md)<br>Match fields whose descriptor exactly equals searchName. Useful for unnamed fields with a distinctive primitive or array descriptor. Use ownerName to narrow to a specific class — strongly recommended here since primitive descriptors like &quot;J&quot; will appear across many classes. |

## Functions

| Name | Summary |
|---|---|
| [valueOf](value-of.md) | [Kraken API]<br>open fun [valueOf](value-of.md)(name: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)): [SearchStrategy](index.md)<br>Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.) |
| [values](values.md) | [Kraken API]<br>open fun [values](values.md)(): [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[SearchStrategy](index.md)&gt;<br>Returns an array containing the constants of this enum type, in the order they're declared. This method may be used to iterate over the constants. |
