//[lib](../../../index.md)/[com.kraken.api.query.player](../index.md)/[LocalPlayerEntity](index.md)/[getExperience](get-experience.md)

# getExperience

[Kraken API]\
open fun [getExperience](get-experience.md)(skill: Skill): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)

Retrieves the total experience of the specified skill for the current client. 

 This method executes a thread-safe operation to fetch the experience points associated with the given Skill object by running on the client thread. 

#### Return

the total experience points of the specified skill.

#### Parameters

Kraken API

| | |
|---|---|
| skill | the skill whose experience is to be retrieved. This parameter must not be null. |
