//[kraken-api](../../../index.md)/[com.kraken.api.service.util](../index.md)/[TaskChain](index.md)/[execute](execute.md)

# execute

[Kraken API]\
open fun [execute](execute.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Executes the task chain sequentially, proceeding through each task until all tasks are complete, an error occurs, or the chain is interrupted. 

The method polls tasks from an internal task queue and executes them in the order they are added. If any task fails (e.g., returns `false`) or an [InterruptedException](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/InterruptedException.html) occurs, the chain halts execution and returns `false`. 

This method performs tasks synchronously, blocking until the completion or failure of all tasks in the chain.

#### Return

`true` if all tasks in the chain are executed successfully; `false` if any task fails or the chain is interrupted.
