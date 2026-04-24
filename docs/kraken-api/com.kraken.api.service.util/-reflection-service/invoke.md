//[kraken-api](../../../index.md)/[com.kraken.api.service.util](../index.md)/[ReflectionService](index.md)/[invoke](invoke.md)

# invoke

[Kraken API]\
open fun [invoke](invoke.md)(className: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html), methodName: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html), garbageValue: [Integer](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Integer.html), instance: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html), args: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)&gt;): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)

Invokes a method specified by the given class and method name. 

 This method automatically handles garbage values if the mapping specifies one, appending it to the arguments list.

#### Return

The result of the method invocation, or null if an error occurs.

#### Parameters

Kraken API

| | |
|---|---|
| className | The obfuscated class name containing the method. |
| methodName | The obfuscated method name. |
| garbageValue | Optional trailing garbage value required by the client method. |
| instance | The object instance to invoke the method on. |
| args | The arguments to pass to the method. |
