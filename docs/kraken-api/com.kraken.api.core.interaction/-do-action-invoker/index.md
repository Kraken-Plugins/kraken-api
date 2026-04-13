//[kraken-api](../../../index.md)/[com.kraken.api.core.interaction](../index.md)/[DoActionInvoker](index.md)

# DoActionInvoker

[Kraken API]\
open class [DoActionInvoker](index.md)

Encapsulates the reflection-based invocation of the RuneLite doAction method. Caches the resolved class and method after the first successful lookup.

## Constructors

| | |
|---|---|
| [DoActionInvoker](-do-action-invoker.md) | [Kraken API]<br>constructor() |

## Functions

| Name | Summary |
|---|---|
| [invoke](invoke.md) | [Kraken API]<br>open fun [invoke](invoke.md)(param0: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), param1: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), opcode: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), identifier: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), itemId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), worldViewId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), option: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html), target: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html), canvasX: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), canvasY: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html))<br>Invokes the *doAction* method through reflection, passing the provided parameters. |
