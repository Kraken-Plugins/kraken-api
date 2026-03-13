//[kraken-api](../../../index.md)/[com.kraken.api.service.util](../index.md)/[TaskChain](index.md)/[delayTicks](delay-ticks.md)

# delayTicks

[Kraken API]\
open fun [delayTicks](delay-ticks.md)(ticks: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [TaskChain](index.md)

Adds a delay to the task chain, pausing execution for the specified duration measured in game ticks before proceeding to the next task. 

A tick is typically defined as the basic unit of game time, depending on the underlying game architecture. This method schedules a task that introduces a delay by syncing with the @SleepService, which ensures proper integration with the game's scheduling system.

#### Return

The current instance of @TaskChain, allowing for method chaining of additional tasks.

#### Parameters

Kraken API

| | |
|---|---|
| ticks | The number of game ticks to delay execution. Must be a positive integer; providing a value less than or equal to 0 may result in undefined behavior. |
