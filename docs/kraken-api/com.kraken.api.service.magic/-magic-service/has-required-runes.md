//[kraken-api](../../../index.md)/[com.kraken.api.service.magic](../index.md)/[MagicService](index.md)/[hasRequiredRunes](has-required-runes.md)

# hasRequiredRunes

[Kraken API]\
open fun [hasRequiredRunes](has-required-runes.md)(spell: [CastableSpell](../-castable-spell/index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Checks if the player has the required runes to cast a given spell. 

 This method verifies the rune requirements for the provided spell by considering: 

- Runes available in the base rune pouch.
- Runes available in the player's inventory, accounting for combination runes that can act as substitutes for their base elemental runes.

 If the rune requirements are met and the spell is deemed castable by the client, the method will return true.

#### Return

`true` if the player has the necessary runes and the spell is castable; `false` otherwise.

#### Parameters

Kraken API

| | |
|---|---|
| spell | The `CastableSpell` representing the spell to check. Contains information about the rune requirements and castability. |
