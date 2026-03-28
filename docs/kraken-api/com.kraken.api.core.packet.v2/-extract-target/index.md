//[kraken-api](../../../index.md)/[com.kraken.api.core.packet.v2](../index.md)/[ExtractTarget](index.md)

# ExtractTarget

[Kraken API]\
enum [ExtractTarget](index.md)

## Entries

| | |
|---|---|
| [OBFUSCATED_NAME](-o-b-f-u-s-c-a-t-e-d_-n-a-m-e/index.md) | [Kraken API]<br>[OBFUSCATED_NAME](-o-b-f-u-s-c-a-t-e-d_-n-a-m-e/index.md)<br>The obfuscated name of the field/method/class itself |
| [GETTER](-g-e-t-t-e-r/index.md) | [Kraken API]<br>[GETTER](-g-e-t-t-e-r/index.md)<br>The getter multiplier on a field |
| [SETTER](-s-e-t-t-e-r/index.md) | [Kraken API]<br>[SETTER](-s-e-t-t-e-r/index.md)<br>The setter multiplier on a field |
| [GARBAGE_VALUE](-g-a-r-b-a-g-e_-v-a-l-u-e/index.md) | [Kraken API]<br>[GARBAGE_VALUE](-g-a-r-b-a-g-e_-v-a-l-u-e/index.md)<br>The garbage value on a method |
| [OWNER_OBFUSCATED_NAME](-o-w-n-e-r_-o-b-f-u-s-c-a-t-e-d_-n-a-m-e/index.md) | [Kraken API]<br>[OWNER_OBFUSCATED_NAME](-o-w-n-e-r_-o-b-f-u-s-c-a-t-e-d_-n-a-m-e/index.md)<br>The obfuscated name of the class that OWNS this method |
| [DESCRIPTOR_CLASS](-d-e-s-c-r-i-p-t-o-r_-c-l-a-s-s/index.md) | [Kraken API]<br>[DESCRIPTOR_CLASS](-d-e-s-c-r-i-p-t-o-r_-c-l-a-s-s/index.md)<br>Parse &quot;Lsome/ClassName;&quot; from a field's descriptor → returns &quot;some/ClassName&quot; |

## Functions

| Name | Summary |
|---|---|
| [valueOf](value-of.md) | [Kraken API]<br>open fun [valueOf](value-of.md)(name: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)): [ExtractTarget](index.md)<br>Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.) |
| [values](values.md) | [Kraken API]<br>open fun [values](values.md)(): [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[ExtractTarget](index.md)&gt;<br>Returns an array containing the constants of this enum type, in the order they're declared. This method may be used to iterate over the constants. |
