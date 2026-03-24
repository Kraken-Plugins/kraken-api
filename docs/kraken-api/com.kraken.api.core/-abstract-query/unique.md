//[kraken-api](../../../index.md)/[com.kraken.api.core](../index.md)/[AbstractQuery](index.md)/[unique](unique.md)

# unique

[Kraken API]\
open fun [unique](unique.md)(): [Q](index.md)

Ensures that only unique elements based on their IDs are included in the stream. 

 This method acts as a wrapper around the `distinctById()` method to provide a concise alias. It guarantees that the resulting stream contains only distinct elements whose IDs have not previously appeared in the stream. 

#### Return

Q A filtered stream containing only unique elements based on their IDs.
