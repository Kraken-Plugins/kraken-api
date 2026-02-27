//[lib](../../../index.md)/[com.kraken.api.query.player](../index.md)/[LocalPlayerEntity](index.md)/[logout](logout.md)

# logout

[Kraken API]\
open fun [logout](logout.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Logs the current player out of the client. If the player is in a place where they cannot be logged out this method will NOT re-attempt to log the player out (i.e. the player was recently in combat).

#### Return

True if the logout was successful and false otherwise.
