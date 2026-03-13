//[kraken-api](../../../index.md)/[com.kraken.api.service.util](../index.md)/[SleepService](index.md)/[sleepUntilTrue](sleep-until-true.md)

# sleepUntilTrue

[Kraken API]\
open fun [sleepUntilTrue](sleep-until-true.md)(awaitedCondition: [BooleanSupplier](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/function/BooleanSupplier.html), time: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), timeout: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Waits until the specified condition is true, checking at a given interval, until a timeout is reached.

#### Return

true if the condition was met, false if the timeout was reached

#### Parameters

Kraken API

| | |
|---|---|
| awaitedCondition | the condition to be met |
| time | the time to sleep between checks in milliseconds |
| timeout | the maximum time to wait in milliseconds |

[Kraken API]\
open fun [sleepUntilTrue](sleep-until-true.md)(awaitedCondition: [BooleanSupplier](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/function/BooleanSupplier.html), timeout: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Waits until the specified condition is true, checking every 100ms, until a timeout is reached.

#### Return

true if the condition was met, false if the timeout was reached

#### Parameters

Kraken API

| | |
|---|---|
| awaitedCondition | the condition to be met |
| timeout | the maximum time to wait in milliseconds |
