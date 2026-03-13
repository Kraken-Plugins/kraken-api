//[kraken-api](../../../index.md)/[com.kraken.api.query.player](../index.md)/[LocalPlayerEntity](index.md)/[getBoostedLevel](get-boosted-level.md)

# getBoostedLevel

[Kraken API]\
open fun [getBoostedLevel](get-boosted-level.md)(skill: Skill): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)

Retrieves the boosted level of a specified skill. 

 This method executes on the client thread to safely fetch the current boosted level of the provided skill from the client.

#### Return

The boosted level of the specified skill as an integer.

#### Parameters

Kraken API

| | |
|---|---|
| skill | The @Skill object representing the skill for which the boosted level is needed. Must not be null. |
