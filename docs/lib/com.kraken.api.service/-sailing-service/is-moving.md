//[lib](../../../index.md)/[com.kraken.api.service](../index.md)/[SailingService](index.md)/[isMoving](is-moving.md)

# isMoving

[Kraken API]\
open fun [isMoving](is-moving.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Checks if the boat is currently moving based on the speed state. 

 This method determines the movement state by examining the value of a specific varbit linked to the boat's speed. A non-zero value indicates that the boat is in motion, while a zero value means it is stationary. 

#### Return

true if the boat is moving; false otherwise.
