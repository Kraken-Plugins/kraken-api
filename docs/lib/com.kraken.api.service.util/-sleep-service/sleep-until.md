//[lib](../../../index.md)/[com.kraken.api.service.util](../index.md)/[SleepService](index.md)/[sleepUntil](sleep-until.md)

# sleepUntil

[Kraken API]\
open fun [sleepUntil](sleep-until.md)(condition: [Supplier](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/function/Supplier.html)&lt;[Boolean](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Boolean.html)&gt;)

Waits until the specified condition is true.

#### Parameters

Kraken API

| | |
|---|---|
| condition | the condition to be met |

[Kraken API]\
open fun [sleepUntil](sleep-until.md)(condition: [Supplier](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/function/Supplier.html)&lt;[Boolean](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Boolean.html)&gt;, timeoutMS: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

sleeps until the specified condition is true or the timeout is reached.

#### Return

true if the condition was met, false if the timeout was reached

#### Parameters

Kraken API

| | |
|---|---|
| condition | the condition to be met |
| timeoutMS | the maximum time to sleep in milliseconds |

[Kraken API]\
open fun [sleepUntil](sleep-until.md)(condition: [Supplier](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/function/Supplier.html)&lt;[Boolean](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Boolean.html)&gt;, ticks: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

sleeps until the specified condition is true or the timeout is reached.

#### Return

true if the condition was met, false if the timeout was reached

#### Parameters

Kraken API

| | |
|---|---|
| condition | the condition to be met |
| ticks | the maximum time to sleep in game ticks |

[Kraken API]\
open fun [sleepUntil](sleep-until.md)(awaitedCondition: [BooleanSupplier](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/function/BooleanSupplier.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Waits until the specified condition is true, with a default timeout of 5000ms.

#### Return

true if the condition was met, false if the timeout was reached

#### Parameters

Kraken API

| | |
|---|---|
| awaitedCondition | the condition to be met |

[Kraken API]\
open fun [sleepUntil](sleep-until.md)(awaitedCondition: [BooleanSupplier](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/function/BooleanSupplier.html), time: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Waits until the specified condition is true, or a timeout is reached.

#### Return

true if the condition was met, false if the timeout was reached

#### Parameters

Kraken API

| | |
|---|---|
| awaitedCondition | the condition to be met |
| time | the maximum time to wait in milliseconds |
