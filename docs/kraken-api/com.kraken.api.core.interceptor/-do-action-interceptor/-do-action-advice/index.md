//[kraken-api](../../../../index.md)/[com.kraken.api.core.interceptor](../../index.md)/[DoActionInterceptor](../index.md)/[DoActionAdvice](index.md)

# DoActionAdvice

[Kraken API]\
open class [DoActionAdvice](index.md)

Advice injected into the obfuscated doAction method. We do NOT skip the method here; we just snoop the parameters for mapping.

## Constructors

| | |
|---|---|
| [DoActionAdvice](-do-action-advice.md) | [Kraken API]<br>constructor() |

## Functions

| Name | Summary |
|---|---|
| [onEnter](on-enter.md) | [Kraken API]<br>open fun [onEnter](on-enter.md)(param0: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), param1: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), opcode: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), id: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), itemId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), worldView: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), option: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html), target: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html), mouseX: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), mouseY: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), garbageValue: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)) |
