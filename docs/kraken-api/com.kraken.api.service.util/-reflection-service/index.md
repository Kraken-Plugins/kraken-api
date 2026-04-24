//[kraken-api](../../../index.md)/[com.kraken.api.service.util](../index.md)/[ReflectionService](index.md)

# ReflectionService

[Kraken API]\
open class [ReflectionService](index.md)

Service for handling reflection operations, including field access and method invocation. 

 This service maintains a cache of reflected fields and methods to improve performance for repeated accesses. It supports accessing obfuscated members via the class and member names mapped in `packets.json`.

## Constructors

| | |
|---|---|
| [ReflectionService](-reflection-service.md) | [Kraken API]<br>constructor(client: Client)<br>Constructs a new ReflectionService. |

## Functions

| Name | Summary |
|---|---|
| [getFieldValue](get-field-value.md) | [Kraken API]<br>open fun &lt;[T](get-field-value.md)&gt; [getFieldValue](get-field-value.md)(className: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html), fieldName: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html), instance: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)): [T](get-field-value.md)<br>Retrieves the value of a field specified by the given class and field name. |
| [invoke](invoke.md) | [Kraken API]<br>open fun [invoke](invoke.md)(className: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html), methodName: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html), garbageValue: [Integer](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Integer.html), instance: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html), args: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)&gt;): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)<br>Invokes a method specified by the given class and method name. |
| [setFieldValue](set-field-value.md) | [Kraken API]<br>open fun [setFieldValue](set-field-value.md)(className: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html), fieldName: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html), instance: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html), value: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html))<br>Sets the value of a field specified by the given class and field name. |
