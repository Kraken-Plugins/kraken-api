//[lib](../../../index.md)/[com.kraken.api.service.util.reflect](../index.md)/[ReflectionService](index.md)/[invoke](invoke.md)

# invoke

[Kraken API]\
open fun [invoke](invoke.md)(hook: [MethodHook](../../com.kraken.api.service.util.reflect.hooks.model/-method-hook/index.md), instance: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html), args: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)&gt;): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)

Invokes a method specified by the given hook. 

 This method automatically handles garbage values if the hook specifies one, appending it to the arguments list.

#### Return

The result of the method invocation, or null if an error occurs.

#### Parameters

Kraken API

| | |
|---|---|
| hook | The [MethodHook](../../com.kraken.api.service.util.reflect.hooks.model/-method-hook/index.md) defining the class and method name. |
| instance | The object instance to invoke the method on. |
| args | The arguments to pass to the method. |
