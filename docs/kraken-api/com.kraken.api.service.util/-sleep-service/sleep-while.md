//[kraken-api](../../../index.md)/[com.kraken.api.service.util](../index.md)/[SleepService](index.md)/[sleepWhile](sleep-while.md)

# sleepWhile

[Kraken API]\
open fun [sleepWhile](sleep-while.md)(condition: [BooleanSupplier](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/function/BooleanSupplier.html), timeout: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Sleeps while the specified condition is true or until the timeout is reached.

#### Return

true if the condition became false, false if the timeout was reached

#### Parameters

Kraken API

| | |
|---|---|
| condition | the condition to be met |
| timeout | the maximum time to sleep in milliseconds |
