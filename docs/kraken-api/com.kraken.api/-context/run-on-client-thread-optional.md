//[kraken-api](../../../index.md)/[com.kraken.api](../index.md)/[Context](index.md)/[runOnClientThreadOptional](run-on-client-thread-optional.md)

# runOnClientThreadOptional

[Kraken API]\
open fun &lt;[T](run-on-client-thread-optional.md)&gt; [runOnClientThreadOptional](run-on-client-thread-optional.md)(method: [Callable](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/concurrent/Callable.html)&lt;[T](run-on-client-thread-optional.md)&gt;): [Optional](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/Optional.html)&lt;[T](run-on-client-thread-optional.md)&gt;

Run a method on the client thread, returning an optional of the result.

#### Return

The result from the called method

#### Parameters

Kraken API

| | |
|---|---|
| method | The method to call |
| &lt;T&gt; | The type of the method's return value |
