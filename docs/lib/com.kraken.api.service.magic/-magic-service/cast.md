//[lib](../../../index.md)/[com.kraken.api.service.magic](../index.md)/[MagicService](index.md)/[cast](cast.md)

# cast

[Kraken API]\
open fun [cast](cast.md)(spell: [CastableSpell](../-castable-spell/index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Casts the specified spell, if it is valid and the necessary conditions are met. 

 This method validates whether the spell can be cast, determines the appropriate action (especially for teleport spell variants), and enqueues the required packets to perform the spell cast. 

 Note that this method handles teleport CastableSpell with multiple variants (e.g., Varrock Teleport vs. Grand Exchange Teleport) and calculates the correct action based on the player's configuration.

#### Return

@true if the spell was successfully cast, @false otherwise. 

Returns @false if the spell is invalid, cannot be cast, or if required conditions (e.g., runes) are not met.

#### Parameters

Kraken API

| | |
|---|---|
| spell | The @CastableSpell instance representing the spell to be cast. Must not be null. <br>- For teleport CastableSpell with variants, the variant action will be determined dynamically. - Ensure the correct spell is passed to avoid unintended behavior. |
