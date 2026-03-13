//[kraken-api](../../../index.md)/[com.kraken.api.service.util](../index.md)/[TaskChain](index.md)/[waitUntil](wait-until.md)

# waitUntil

[Kraken API]\
open fun [waitUntil](wait-until.md)(condition: [BooleanSupplier](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/function/BooleanSupplier.html), checkDelayMs: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), timeoutMs: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [TaskChain](index.md)

Waits until the given condition is met or a timeout occurs, checking the condition periodically at the specified interval. If the condition is not met within the timeout period, the task chain will stop execution and log a warning. 

This method continuously evaluates the `condition` on the client thread using the `runOnClientThread` method provided by the `Context`. The evaluation is performed in a thread-safe manner, with the specified delay between checks.

#### Return

the current instance of @TaskChain, allowing for further chaining of tasks.

#### Parameters

Kraken API

| | |
|---|---|
| condition | a @BooleanSupplier representing the condition to wait for. The condition is evaluated on the client thread. Cannot be null. A null value may result in unexpected runtime behavior. |
| checkDelayMs | the delay (in milliseconds) between subsequent evaluations of the `condition`. This determines how frequently the condition is checked. |
| timeoutMs | the maximum time (in milliseconds) to wait for the `condition` to evaluate to `true`. If the condition is not met within this duration, the chain execution terminates. |

[Kraken API]\
open fun [waitUntil](wait-until.md)(condition: [BooleanSupplier](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/function/BooleanSupplier.html)): [TaskChain](index.md)

Waits until the given condition is met, using default delay and timeout values. If the condition is not met within the default timeout period, the task chain will stop execution and log a warning. 

This method evaluates the `condition` on the client thread and continues to the next task in the chain when the condition is satisfied. The evaluations are performed in a thread-safe manner.

#### Return

the current instance of @TaskChain, allowing for method chaining of additional tasks.

#### Parameters

Kraken API

| | |
|---|---|
| condition | a @BooleanSupplier representing the condition to wait for. The condition is evaluated on the client thread. Cannot be null. A null value may result in unexpected runtime behavior. |

[Kraken API]\
open fun [waitUntil](wait-until.md)(condition: [BooleanSupplier](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/function/BooleanSupplier.html), timeout: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [TaskChain](index.md)

Waits until the given condition is met or a timeout occurs. The condition is checked periodically at a default interval. If the condition is not met within the timeout period, the task chain will stop execution and log a warning. 

This method evaluates the `condition` on the client thread and continues to the next task in the chain when the condition is satisfied. The evaluations are performed in a thread-safe manner using the `runOnClientThread` method of the `Context`.

#### Return

the current instance of @TaskChain, allowing for method chaining of additional tasks.

#### Parameters

Kraken API

| | |
|---|---|
| condition | a @BooleanSupplier representing the condition to wait for. The condition is evaluated on the client thread. Cannot be null. A null value may result in unintended runtime behavior. |
| timeout | the maximum time (in milliseconds) to wait for the `condition` to evaluate to `true`. If the condition is not met within this duration, the chain execution terminates. |
