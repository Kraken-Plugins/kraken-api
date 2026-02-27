//[lib](../../../index.md)/[com.kraken.api.simulation](../index.md)/[SimulationDecisionAdapter](index.md)/[execute](execute.md)

# execute

[Kraken API]\
open fun [execute](execute.md)(action: [SimulationDecisionAdapter.ExecutableAction](-executable-action/index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Executes all action steps in order.

#### Return

true when at least one step was executed successfully.

#### Parameters

Kraken API

| | |
|---|---|
| action | executable action to perform. |

[Kraken API]\
open fun [execute](execute.md)(action: [SimulationDecisionAdapter.ExecutableAction](-executable-action/index.md), allowedStepTypes: [Set](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/Set.html)&lt;[SimulationDecisionAdapter.ExecutableStepType](-executable-step-type/index.md)&gt;): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Executes all action steps in order, filtered by allowed step types.

#### Return

true when at least one permitted step was executed successfully.

#### Parameters

Kraken API

| | |
|---|---|
| action | executable action to perform. |
| allowedStepTypes | set of step types permitted for execution. |
