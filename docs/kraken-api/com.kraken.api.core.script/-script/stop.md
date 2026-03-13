//[kraken-api](../../../index.md)/[com.kraken.api.core.script](../index.md)/[Script](index.md)/[stop](stop.md)

# stop

[Kraken API]\
open fun [stop](stop.md)(callback: [Runnable](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Runnable.html))

Stops the current execution loop and performs the necessary cleanup. This method safely shuts down the process, unregisters the instance from the event bus, and triggers the provided callback after successful termination. 

Behavior:

- Sets the running status to `false` if the process is active.
- Unregisters the instance from the event bus.
- Cancels the associated `RunnableTask`.
- Waits for the asynchronous `future` to complete before invoking the `callback`.

#### Parameters

Kraken API

| | |
|---|---|
| callback | A `Runnable` that will execute after the stop operation is complete; can be `null` if no action is required after stopping. |

[Kraken API]\
open fun [stop](stop.md)()

Gracefully stops a running asynchronous loop.
