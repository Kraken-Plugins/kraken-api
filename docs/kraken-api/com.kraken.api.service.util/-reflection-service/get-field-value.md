//[kraken-api](../../../index.md)/[com.kraken.api.service.util](../index.md)/[ReflectionService](index.md)/[getFieldValue](get-field-value.md)

# getFieldValue

[Kraken API]\
open fun &lt;[T](get-field-value.md)&gt; [getFieldValue](get-field-value.md)(className: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html), fieldName: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html), instance: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)): [T](get-field-value.md)

Retrieves the value of a field specified by the given class and field name.

#### Return

The value of the field, or null if an error occurs.

#### Parameters

Kraken API

| | |
|---|---|
| className | The obfuscated class name containing the field. |
| fieldName | The obfuscated field name. |
| instance | The object instance to retrieve the field value from. |
| &lt;T&gt; | The expected type of the field value. |
