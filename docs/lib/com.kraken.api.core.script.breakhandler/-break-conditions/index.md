//[lib](../../../index.md)/[com.kraken.api.core.script.breakhandler](../index.md)/[BreakConditions](index.md)

# BreakConditions

[Kraken API]\
open class [BreakConditions](index.md)

## Constructors

| | |
|---|---|
| [BreakConditions](-break-conditions.md) | [Kraken API]<br>constructor() |

## Functions

| Name | Summary |
|---|---|
| [atSpecificTime](at-specific-time.md) | [Kraken API]<br>open fun [atSpecificTime](at-specific-time.md)(hourOfDay: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), minute: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [BreakCondition](../-break-condition/index.md)<br>Breaks at a specific time of day. |
| [customCondition](custom-condition.md) | [Kraken API]<br>open fun [customCondition](custom-condition.md)(shouldBreakCheck: [Supplier](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/function/Supplier.html)&lt;[Boolean](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Boolean.html)&gt;, reason: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)): [BreakCondition](../-break-condition/index.md)<br>A custom break condition which can include any logic |
| [onExperienceGained](on-experience-gained.md) | [Kraken API]<br>open fun [onExperienceGained](on-experience-gained.md)(client: Client, skill: Skill, expThreshold: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [BreakCondition](../-break-condition/index.md)<br>Breaks when experience gained exceeds a threshold. |
| [onLevelReached](on-level-reached.md) | [Kraken API]<br>open fun [onLevelReached](on-level-reached.md)(client: Client, skill: Skill, targetLevel: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [BreakCondition](../-break-condition/index.md)<br>Breaks when a specific skill reaches a target level. |
| [onMaterialDepleted](on-material-depleted.md) | [Kraken API]<br>open fun [onMaterialDepleted](on-material-depleted.md)(ctx: [Context](../../com.kraken.api/-context/index.md), itemId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [BreakCondition](../-break-condition/index.md)<br>Breaks when items in the bank run out (e.g., materials). |
| [runOnce](run-once.md) | [Kraken API]<br>open fun [runOnce](run-once.md)(condition: [BreakCondition](../-break-condition/index.md)): [BreakCondition](../-break-condition/index.md)<br>Wraps a condition to ensure it only triggers once per session. |
