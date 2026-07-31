//[kraken-api](../../../index.md)/[com.kraken.api.simulation.colosseum.live](../index.md)/[ColoExecutor](index.md)/[execute](execute.md)

# execute

[Kraken API]\
open fun [execute](execute.md)(decision: [ColoDecision](../../com.kraken.api.simulation.colosseum.plan/-colo-decision/index.md), loadout: [LoadoutConfig](../../com.kraken.api.simulation.colosseum/-loadout-config/index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Executes every component of the decision for this tick.

#### Return

true when at least one action was issued.

#### Parameters

Kraken API

| | |
|---|---|
| decision | planner decision. |
| loadout | loadout used to resolve item ids. |
