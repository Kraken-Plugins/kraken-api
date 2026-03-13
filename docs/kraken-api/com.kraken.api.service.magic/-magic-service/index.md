//[kraken-api](../../../index.md)/[com.kraken.api.service.magic](../index.md)/[MagicService](index.md)

# MagicService

[Kraken API]\
open class [MagicService](index.md)

## Constructors

| | |
|---|---|
| [MagicService](-magic-service.md) | [Kraken API]<br>constructor() |

## Functions

| Name | Summary |
|---|---|
| [canCast](can-cast.md) | [Kraken API]<br>open fun [canCast](can-cast.md)(spell: [CastableSpell](../-castable-spell/index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Determines whether the specified spell can be cast by the player. |
| [cast](cast.md) | [Kraken API]<br>open fun [cast](cast.md)(spell: [CastableSpell](../-castable-spell/index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Casts the specified spell, if it is valid and the necessary conditions are met. |
| [castOn](cast-on.md) | [Kraken API]<br>open fun [castOn](cast-on.md)(spell: [CastableSpell](../-castable-spell/index.md), target: GameObject): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Casts a spell on a specified target object.<br>[Kraken API]<br>open fun [castOn](cast-on.md)(spell: [CastableSpell](../-castable-spell/index.md), target: NPC): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Attempts to cast the given spell on a specified NPC target.<br>[Kraken API]<br>open fun [castOn](cast-on.md)(spell: [CastableSpell](../-castable-spell/index.md), target: Widget): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Attempts to cast the given spell on a specified widget target. |
| [getRunes](get-runes.md) | [Kraken API]<br>open fun [getRunes](get-runes.md)(): [Map](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/Map.html)&lt;[Rune](../../com.kraken.api.service.magic.rune/-rune/index.md), [Integer](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Integer.html)&gt;<br>Returns the number and type of runes the player has in their inventory and rune pouch combined. |
| [hasRequiredRunes](has-required-runes.md) | [Kraken API]<br>open fun [hasRequiredRunes](has-required-runes.md)(spell: [CastableSpell](../-castable-spell/index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Checks if the player has the required runes to cast a given spell. |
