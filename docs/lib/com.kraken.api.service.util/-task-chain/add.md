//[lib](../../../index.md)/[com.kraken.api.service.util](../index.md)/[TaskChain](index.md)/[add](add.md)

# add

[Kraken API]\
open fun [add](add.md)(otherChain: [TaskChain](index.md)): [TaskChain](index.md)

Adds all tasks from the specified @TaskChain to the current task chain. 

This method incorporates all tasks from the given @TaskChain instance, ensuring they are executed sequentially as part of the current task chain. The added tasks are executed in the order they exist in `otherChain`, maintaining their original sequence within the chain.

#### Return

the current instance of @TaskChain, allowing for method chaining of additional tasks.

#### Parameters

Kraken API

| | |
|---|---|
| otherChain | the @TaskChain whose tasks are to be added to the current chain. Cannot be null. If `null` is passed, the behavior is undefined and may result in a runtime exception. |
