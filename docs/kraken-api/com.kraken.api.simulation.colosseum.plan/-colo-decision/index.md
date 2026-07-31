//[kraken-api](../../../index.md)/[com.kraken.api.simulation.colosseum.plan](../index.md)/[ColoDecision](index.md)

# ColoDecision

[Kraken API]\
class [ColoDecision](index.md)

The planner's output for one tick: everything the executor should do right now, plus the evidence (scores, predicted path, reasoning) the debug overlay renders.

## Functions

| Name | Summary |
|---|---|
| [candidateCount](candidate-count.md) | [Kraken API]<br>open fun [candidateCount](candidate-count.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [candidateScore](candidate-score.md) | [Kraken API]<br>open fun [candidateScore](candidate-score.md)(index: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Double](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-double/index.html) |
| [candidateWorldPoint](candidate-world-point.md) | [Kraken API]<br>open fun [candidateWorldPoint](candidate-world-point.md)(index: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): WorldPoint |
| [getMoveDestination](get-move-destination.md) | [Kraken API]<br>open fun [getMoveDestination](get-move-destination.md)(): WorldPoint |
| [getPrayerToActivate](get-prayer-to-activate.md) | [Kraken API]<br>open fun [getPrayerToActivate](get-prayer-to-activate.md)(): Prayer |
| [hasAttack](has-attack.md) | [Kraken API]<br>open fun [hasAttack](has-attack.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [isEatCombo](is-eat-combo.md) | [Kraken API]<br>open fun [isEatCombo](is-eat-combo.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [isEatFood](is-eat-food.md) | [Kraken API]<br>open fun [isEatFood](is-eat-food.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [isPrayerOff](is-prayer-off.md) | [Kraken API]<br>open fun [isPrayerOff](is-prayer-off.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [isRun](is-run.md) | [Kraken API]<br>open fun [isRun](is-run.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [isSipBrew](is-sip-brew.md) | [Kraken API]<br>open fun [isSipBrew](is-sip-brew.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [isSipRestore](is-sip-restore.md) | [Kraken API]<br>open fun [isSipRestore](is-sip-restore.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [isUseSpec](is-use-spec.md) | [Kraken API]<br>open fun [isUseSpec](is-use-spec.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [plannedPathWorld](planned-path-world.md) | [Kraken API]<br>open fun [plannedPathWorld](planned-path-world.md)(): [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;WorldPoint&gt; |
