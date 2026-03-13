//[kraken-api](../../../index.md)/[com.kraken.api.service](../index.md)/[SailingService](index.md)/[isNavigating](is-navigating.md)

# isNavigating

[Kraken API]\
open fun [isNavigating](is-navigating.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Determines whether the player is at the helm of the boat and navigating 

 This method checks the value of a specific varbit to identify if the navigation system is active. A non-zero value indicates that the boat is navigating, while a value of zero suggests it is not. 

#### Return

true if the boat's navigation system is active; false otherwise.
