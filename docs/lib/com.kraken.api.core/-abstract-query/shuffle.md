//[lib](../../../index.md)/[com.kraken.api.core](../index.md)/[AbstractQuery](index.md)/[shuffle](shuffle.md)

# shuffle

[Kraken API]\
open fun [shuffle](shuffle.md)(): [Stream](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/stream/Stream.html)&lt;[T](index.md)&gt;

Randomizes the order of elements in the stream and returns a new stream with the shuffled elements. 

 This method collects all elements in the stream into a list, shuffles the list using `Collections.shuffle(List)`, and then converts the shuffled list back into a stream for further use. 

 Note: This operation consumes the original stream, making it unsuitable for re-use after calling this method. Additionally, the shuffling process may impact performance for very large datasets due to memory usage (as it fully materializes the stream into a list) and the shuffling algorithm. 

#### Return

A new `Stream<T>` containing the same elements as the original stream, but in a randomized order.
