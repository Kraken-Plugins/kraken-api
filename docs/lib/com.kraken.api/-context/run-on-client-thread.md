//[lib](../../../index.md)/[com.kraken.api](../index.md)/[Context](index.md)/[runOnClientThread](run-on-client-thread.md)

# runOnClientThread

[Kraken API]\
open fun &lt;[T](run-on-client-thread.md)&gt; [runOnClientThread](run-on-client-thread.md)(method: [Callable](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/concurrent/Callable.html)&lt;[T](run-on-client-thread.md)&gt;): [T](run-on-client-thread.md)

Run a method on the client thread, returning the result directly.

#### Return

The result from the called method

#### Parameters

Kraken API

| | |
|---|---|
| method | The method to call |
| &lt;T&gt; | The type of the method's return value |

[Kraken API]\
open fun [runOnClientThread](run-on-client-thread.md)(method: [Runnable](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Runnable.html))

Runs a method on the client thread without returning a result.

#### Parameters

Kraken API

| | |
|---|---|
| method | Runnable method to execute |
