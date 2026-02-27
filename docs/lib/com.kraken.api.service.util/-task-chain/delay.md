//[lib](../../../index.md)/[com.kraken.api.service.util](../index.md)/[TaskChain](index.md)/[delay](delay.md)

# delay

[Kraken API]\
open fun [delay](delay.md)(ms: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [TaskChain](index.md)

Adds a delay to the task chain, pausing execution for the specified duration in milliseconds before proceeding to the next task. 

This method schedules a task that introduces the delay by invoking Thread.sleep(ms). As a result, chain progression is paused during the delay duration.

#### Return

The current instance of TaskChain, allowing for method chaining of additional tasks.

#### Parameters

Kraken API

| | |
|---|---|
| ms | The duration of the delay in milliseconds. Must be a non-negative integer; a value less than 0 may result in unexpected behavior. |
