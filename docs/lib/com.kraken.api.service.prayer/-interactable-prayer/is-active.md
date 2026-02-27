//[lib](../../../index.md)/[com.kraken.api.service.prayer](../index.md)/[InteractablePrayer](index.md)/[isActive](is-active.md)

# isActive

[Kraken API]\
open fun [isActive](is-active.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Checks whether the current @InteractablePrayer is active. 

A prayer is considered active if its corresponding `varbit` value equals `1` in the game's internal context.

#### Return

`true` if the prayer is active; `false` otherwise.
