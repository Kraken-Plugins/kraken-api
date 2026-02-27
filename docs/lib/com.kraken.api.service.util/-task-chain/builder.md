//[lib](../../../index.md)/[com.kraken.api.service.util](../index.md)/[TaskChain](index.md)/[builder](builder.md)

# builder

[Kraken API]\
open fun [builder](builder.md)(ctx: [Context](../../com.kraken.api/-context/index.md)): [TaskChain](index.md)

Creates a new instance of TaskChain using the specified Context. 

 This method serves as a static factory for initializing a TaskChain with the given Context. The resulting TaskChain can be used to construct and execute a sequence of tasks.

#### Return

a new TaskChain instance initialized with the specified Context.

#### Parameters

Kraken API

| | |
|---|---|
| ctx | the Context used to initialize the TaskChain. This context is typically required for operations that depend on thread-safe execution and client interactions. Cannot be `null`. Passing `null` will result in a `NullPointerException`. |
