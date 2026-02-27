//[lib](../../../index.md)/[com.kraken.api.core.script.breakhandler](../index.md)/[BreakConditions](index.md)/[onExperienceGained](on-experience-gained.md)

# onExperienceGained

[Kraken API]\
open fun [onExperienceGained](on-experience-gained.md)(client: Client, skill: Skill, expThreshold: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [BreakCondition](../-break-condition/index.md)

Breaks when experience gained exceeds a threshold.

#### Return

BreakCondition

#### Parameters

Kraken API

| | |
|---|---|
| client | RuneLite client |
| skill | The target skill for exp tracking |
| expThreshold | The amount of exp needed to trigger the break |
