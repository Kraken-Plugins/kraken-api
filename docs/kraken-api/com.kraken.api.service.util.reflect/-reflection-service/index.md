//[kraken-api](../../../index.md)/[com.kraken.api.service.util.reflect](../index.md)/[ReflectionService](index.md)

# ReflectionService

[Kraken API]\
open class [ReflectionService](index.md)

Service for handling reflection operations, including field access and method invocation. 

 This service maintains a cache of reflected fields and methods to improve performance for repeated accesses. It supports accessing obfuscated members via [FieldHook](../../com.kraken.api.service.util.reflect.hooks.model/-field-hook/index.md) and [MethodHook](../../com.kraken.api.service.util.reflect.hooks.model/-method-hook/index.md) definitions.

## Constructors

| | |
|---|---|
| [ReflectionService](-reflection-service.md) | [Kraken API]<br>constructor(client: Client)<br>Constructs a new ReflectionService. |

## Functions

| Name | Summary |
|---|---|
| [getFieldValue](get-field-value.md) | [Kraken API]<br>open fun &lt;[T](get-field-value.md)&gt; [getFieldValue](get-field-value.md)(hook: [FieldHook](../../com.kraken.api.service.util.reflect.hooks.model/-field-hook/index.md), instance: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)): [T](get-field-value.md)<br>Retrieves the value of a field specified by the given hook. |
| [invoke](invoke.md) | [Kraken API]<br>open fun [invoke](invoke.md)(hook: [MethodHook](../../com.kraken.api.service.util.reflect.hooks.model/-method-hook/index.md), instance: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html), args: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)&gt;): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)<br>Invokes a method specified by the given hook. |
| [setFieldValue](set-field-value.md) | [Kraken API]<br>open fun [setFieldValue](set-field-value.md)(hook: [FieldHook](../../com.kraken.api.service.util.reflect.hooks.model/-field-hook/index.md), instance: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html), value: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html))<br>Sets the value of a field specified by the given hook. |
