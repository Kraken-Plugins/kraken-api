//[kraken-api](../../../index.md)/[com.kraken.api.service.magic](../index.md)/[MagicService](index.md)/[canCast](can-cast.md)

# canCast

[Kraken API]\
open fun [canCast](can-cast.md)(spell: [CastableSpell](../-castable-spell/index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Determines whether the specified spell can be cast by the player. 

This method verifies various conditions required to cast a spell, including: 

- Whether the client packets are properly loaded
- Whether the spell is valid and belongs to the player's current spellbook
- Whether the player's boosted Magic level meets or exceeds the spell's required level
- Whether the player possesses the necessary runes to cast the spell
- For specific CastableSpell requiring prayer, whether the player has sufficient Prayer points

 If any of these conditions fail, the method logs a warning and returns @false.

#### Return

@true if all conditions for casting the spell are met, @false otherwise. 

Returns @false for invalid CastableSpell, mismatched spellbooks, insufficient Magic level, missing runes, or insufficient Prayer points for certain CastableSpell.

#### Parameters

Kraken API

| | |
|---|---|
| spell | The @CastableSpell instance representing the spell to check. <br>- Must not be @null. - The spell must belong to the player's active spellbook to be castable. |
