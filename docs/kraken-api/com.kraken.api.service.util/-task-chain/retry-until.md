//[kraken-api](../../../index.md)/[com.kraken.api.service.util](../index.md)/[TaskChain](index.md)/[retryUntil](retry-until.md)

# retryUntil

[Kraken API]\
open fun [retryUntil](retry-until.md)(action: [Runnable](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Runnable.html), successCondition: [BooleanSupplier](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/function/BooleanSupplier.html), maxRetries: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), retryDelayMs: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [TaskChain](index.md)

Retries a specific action until a condition is met or the maximum number of retries is reached. 

 This is useful for &quot;flaky&quot; interactions, such as clicking a bank booth (which might miss) or attacking an NPC (which might be interrupted). 

#### Return

The current [TaskChain](index.md) instance.

#### Parameters

Kraken API

| | |
|---|---|
| action | The [Runnable](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Runnable.html) action to perform (e.g., `() -> object.interact("Open")`). This is automatically executed on the client thread. |
| successCondition | A [BooleanSupplier](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/function/BooleanSupplier.html) that determines if the action succeeded (e.g., `() -> player.getAnimation() != IDLE`). Checked on the client thread. |
| maxRetries | The maximum number of times to retry the action if the condition returns false. |
| retryDelayMs | The time to wait (in ms) between the action and the check (and before the next retry). This should be at least 600ms (1 tick) for most game actions. |

[Kraken API]\
open fun [retryUntil](retry-until.md)(action: [Runnable](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Runnable.html), successCondition: [BooleanSupplier](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/function/BooleanSupplier.html)): [TaskChain](index.md)

Retries an action until a condition is met using default settings (3 retries, 600ms delay).

#### Return

The current [TaskChain](index.md) instance.

#### Parameters

Kraken API

| | |
|---|---|
| action | The action to perform. |
| successCondition | The condition to check for success. |
