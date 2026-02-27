//[lib](../../../index.md)/[com.kraken.api.core](../index.md)/[AbstractQuery](index.md)/[reverse](reverse.md)

# reverse

[Kraken API]\
open fun [reverse](reverse.md)(): [Stream](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/stream/Stream.html)&lt;[T](index.md)&gt;

Reverses the order of elements in the stream and returns a new stream with the reversed order. 

 This method collects all elements in the stream into a list, reverses the list using `Collections.reverse(List)`, and then converts the reversed list back into a stream for further use. 

 Note: This operation consumes the original stream, making it unsuitable for re-use after calling this method. Additionally, the reversal process may impact performance for very large datasets due to memory usage (as it fully materializes the stream into a list). 

#### Return

A new `Stream<T>` containing the same elements as the original stream, but in reversed order.
