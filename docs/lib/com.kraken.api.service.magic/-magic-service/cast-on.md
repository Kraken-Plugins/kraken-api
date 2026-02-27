//[lib](../../../index.md)/[com.kraken.api.service.magic](../index.md)/[MagicService](index.md)/[castOn](cast-on.md)

# castOn

[Kraken API]\
open fun [castOn](cast-on.md)(spell: [CastableSpell](../-castable-spell/index.md), target: Widget): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Attempts to cast the given spell on a specified widget target. 

 This method verifies if the spell can be cast by invoking the `canCast` method. If the spell is valid and the required conditions are met (e.g., the spell exists in the active spellbook, the player has the necessary items, levels, etc.), it retrieves the spell's widget representation and performs the &quot;use on&quot; action to cast the spell on the target. 

#### Return

@true if the spell was successfully cast on the target, @false otherwise. 

Returns @false in cases where the spell is invalid, conditions necessary for casting are not met, or the target widget is not valid or accessible.

#### Parameters

Kraken API

| | |
|---|---|
| spell | The @CastableSpell instance representing the spell to cast. <br>- Must not be @null. - Should belong to the player's current spellbook and satisfy all requirements for casting. |
| target | The @Widget instance representing the target of the spell. <br>- Must not be @null. - The widget should correspond to a valid in-game target for the selected spell. |

[Kraken API]\
open fun [castOn](cast-on.md)(spell: [CastableSpell](../-castable-spell/index.md), target: NPC): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Attempts to cast the given spell on a specified NPC target. 

This method checks whether the spell can be cast by invoking the `canCast` method. If the spell is valid and all necessary conditions for casting (e.g., current spellbook, required runes, etc.) are satisfied, it retrieves the spell's corresponding widget and performs the &quot;use-on&quot; action to cast the spell on the NPC target.

#### Return

@true if the spell was successfully cast on the NPC target, @false otherwise. 

Returns @false if the spell is invalid, the conditions for casting are not met, the spell's widget cannot be retrieved, or the NPC is not a valid target.

#### Parameters

Kraken API

| | |
|---|---|
| spell | The @CastableSpell instance representing the spell to cast. <br>- Must not be @null. - The spell should exist in the player's current spellbook. - The spell must meet all prerequisites for casting, including level and resource requirements. |
| target | The @NPC instance representing the target of the spell. <br>- Must not be @null. - The NPC must be a valid target for the selected spell. |

[Kraken API]\
open fun [castOn](cast-on.md)(spell: [CastableSpell](../-castable-spell/index.md), target: GameObject): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Casts a spell on a specified target object. 

 This method checks if the specified spell can be cast, retrieves the widget associated with the spell, and then uses it on the provided target object.

#### Return

`true` if the spell was successfully cast on the target, `false` otherwise.

#### Parameters

Kraken API

| | |
|---|---|
| spell | The @CastableSpell object representing the spell to be cast. |
| target | The @GameObject on which the spell will be cast. |
