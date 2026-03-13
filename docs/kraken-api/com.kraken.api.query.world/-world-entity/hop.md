//[kraken-api](../../../index.md)/[com.kraken.api.query.world](../index.md)/[WorldEntity](index.md)/[hop](hop.md)

# hop

[Kraken API]\
open fun [hop](hop.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Attempts to perform a world hop for the current `WorldEntity`. 

 This method interacts with the RuneLite client to hop to the target world associated with this `WorldEntity`. Depending on the client's state, it may handle login screen transitions or directly use the world hopper interface to complete the action. This operation may require the world hopper plugin to be enabled. 

#### Return

`true` if the world hop was successfully performed; `false` otherwise.
