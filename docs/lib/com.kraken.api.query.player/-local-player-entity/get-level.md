//[lib](../../../index.md)/[com.kraken.api.query.player](../index.md)/[LocalPlayerEntity](index.md)/[getLevel](get-level.md)

# getLevel

[Kraken API]\
open fun [getLevel](get-level.md)(skill: Skill): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)

Retrieves the base level of the specified skill. 

 This method executes a client-side operation to obtain the real (unboosted) level of the given skill. The skill must be a valid `Skill` object. 

#### Return

The base level of the specified skill as an integer. If the skill is invalid or cannot be retrieved, the method may return 0 or throw an exception depending on the implementation details.

#### Parameters

Kraken API

| | |
|---|---|
| skill | The `Skill` for which the base level is being queried. Represents one of the player's in-game skills. |
