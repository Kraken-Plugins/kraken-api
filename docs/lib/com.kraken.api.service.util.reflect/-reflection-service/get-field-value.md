//[lib](../../../index.md)/[com.kraken.api.service.util.reflect](../index.md)/[ReflectionService](index.md)/[getFieldValue](get-field-value.md)

# getFieldValue

[Kraken API]\
open fun &lt;[T](get-field-value.md)&gt; [getFieldValue](get-field-value.md)(hook: [FieldHook](../../com.kraken.api.service.util.reflect.hooks.model/-field-hook/index.md), instance: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)): [T](get-field-value.md)

Retrieves the value of a field specified by the given hook.

#### Return

The value of the field, or null if an error occurs.

#### Parameters

Kraken API

| | |
|---|---|
| hook | The [FieldHook](../../com.kraken.api.service.util.reflect.hooks.model/-field-hook/index.md) defining the class and field name. |
| instance | The object instance to retrieve the field value from. |
| &lt;T&gt; | The expected type of the field value. |
