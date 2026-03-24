//[kraken-api](../../../index.md)/[com.kraken.api.core](../index.md)/[AbstractQuery](index.md)/[distinctById](distinct-by-id.md)

# distinctById

[Kraken API]\
open fun [distinctById](distinct-by-id.md)(): [Q](index.md)

Filters the stream of elements, ensuring only unique elements are returned based on their IDs. 

 This method uses a thread-safe Set to track IDs of processed elements. The elements are included in the resulting stream only if their ID has not been seen before. 

**Note:** This operation assumes that each element in the stream has a unique identifier accessible through `getId()`. 

#### Return

Q A filtered stream containing only elements with unique IDs.
