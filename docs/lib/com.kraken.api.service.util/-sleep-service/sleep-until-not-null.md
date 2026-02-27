//[lib](../../../index.md)/[com.kraken.api.service.util](../index.md)/[SleepService](index.md)/[sleepUntilNotNull](sleep-until-not-null.md)

# sleepUntilNotNull

[Kraken API]\
open fun &lt;[T](sleep-until-not-null.md)&gt; [sleepUntilNotNull](sleep-until-not-null.md)(method: [Callable](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/concurrent/Callable.html)&lt;[T](sleep-until-not-null.md)&gt;, timeoutMillis: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), sleepMillis: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [T](sleep-until-not-null.md)

Repeatedly calls a method until it returns a non-null value, or a timeout is reached.

#### Return

the non-null value, or null if the timeout was reached

#### Parameters

Kraken API

| | |
|---|---|
| method | the method to call |
| timeoutMillis | the maximum time to wait in milliseconds |
| sleepMillis | the time to sleep between calls |
| &lt;T&gt; | the return type of the method |

[Kraken API]\
open fun &lt;[T](sleep-until-not-null.md)&gt; [sleepUntilNotNull](sleep-until-not-null.md)(method: [Callable](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/concurrent/Callable.html)&lt;[T](sleep-until-not-null.md)&gt;, timeoutMillis: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [T](sleep-until-not-null.md)

Repeatedly calls a method until it returns a non-null value, or a timeout is reached. Sleeps 100ms between calls.

#### Return

the non-null value, or null if the timeout was reached

#### Parameters

Kraken API

| | |
|---|---|
| method | the method to call |
| timeoutMillis | the maximum time to wait in milliseconds |
| &lt;T&gt; | the return type of the method |
