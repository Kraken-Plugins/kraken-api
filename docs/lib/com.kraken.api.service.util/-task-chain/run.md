//[lib](../../../index.md)/[com.kraken.api.service.util](../index.md)/[TaskChain](index.md)/[run](run.md)

# run

[Kraken API]\
open fun [run](run.md)(action: [Runnable](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Runnable.html)): [TaskChain](index.md)

Adds a task to the chain that executes the specified @Runnable action on the client thread and immediately proceeds to the next task in the chain. 

The action is executed synchronously on the client thread using the @Context's `runOnClientThread` method. This ensures the action is performed in a thread-safe manner with respect to client operations.

#### Return

the current instance of @TaskChain, allowing for method chaining of additional tasks.

#### Parameters

Kraken API

| | |
|---|---|
| action | the @Runnable task to be executed on the client thread. Cannot be null. Throws a @NullPointerException if null is passed. |
