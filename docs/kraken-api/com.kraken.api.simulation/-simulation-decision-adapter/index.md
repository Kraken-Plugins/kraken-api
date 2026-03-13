//[kraken-api](../../../index.md)/[com.kraken.api.simulation](../index.md)/[SimulationDecisionAdapter](index.md)

# SimulationDecisionAdapter

[Kraken API]\
class [SimulationDecisionAdapter](index.md)

Converts decision-tree simulation results into executable game actions.

## Constructors

| | |
|---|---|
| [SimulationDecisionAdapter](-simulation-decision-adapter.md) | [Kraken API]<br>constructor(ctx: [Context](../../com.kraken.api/-context/index.md), movementService: [MovementService](../../com.kraken.api.service.movement/-movement-service/index.md), prayerService: [PrayerService](../../com.kraken.api.service.prayer/-prayer-service/index.md), magicService: [MagicService](../../com.kraken.api.service.magic/-magic-service/index.md))<br>Constructs a decision adapter for converting and executing simulation outcomes. |

## Types

| Name | Summary |
|---|---|
| [AdaptOptions](-adapt-options/index.md) | [Kraken API]<br>class [AdaptOptions](-adapt-options/index.md)<br>Action adaptation options for optional runtime interaction steps. |
| [ExecutableAction](-executable-action/index.md) | [Kraken API]<br>class [ExecutableAction](-executable-action/index.md)<br>Runtime action payload translated from a simulation decision. |
| [ExecutableStep](-executable-step/index.md) | [Kraken API]<br>class [ExecutableStep](-executable-step/index.md)<br>Runtime step payload translated from simulation decisions. |
| [ExecutableStepType](-executable-step-type/index.md) | [Kraken API]<br>enum [ExecutableStepType](-executable-step-type/index.md)<br>Supported executable step types. |

## Functions

| Name | Summary |
|---|---|
| [adapt](adapt.md) | [Kraken API]<br>open fun [adapt](adapt.md)(result: [DecisionTreeSearch.Result](../../com.kraken.api.simulation.tree/-decision-tree-search/-result/index.md), rootState: [SimulationState](../-simulation-state/index.md)): [SimulationDecisionAdapter.ExecutableAction](-executable-action/index.md)<br>Converts a decision result into an executable action with no optional interaction step.<br>[Kraken API]<br>open fun [adapt](adapt.md)(result: [DecisionTreeSearch.Result](../../com.kraken.api.simulation.tree/-decision-tree-search/-result/index.md), rootState: [SimulationState](../-simulation-state/index.md), options: [SimulationDecisionAdapter.AdaptOptions](-adapt-options/index.md)): [SimulationDecisionAdapter.ExecutableAction](-executable-action/index.md)<br>Converts a decision result into an executable action with configurable adaptation options.<br>[Kraken API]<br>open fun [adapt](adapt.md)(result: [DecisionTreeSearch.Result](../../com.kraken.api.simulation.tree/-decision-tree-search/-result/index.md), rootState: [SimulationState](../-simulation-state/index.md), interactionAction: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html), interactionDistance: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [SimulationDecisionAdapter.ExecutableAction](-executable-action/index.md)<br>Converts a decision result into an executable action with optional npc interaction. |
| [execute](execute.md) | [Kraken API]<br>open fun [execute](execute.md)(action: [SimulationDecisionAdapter.ExecutableAction](-executable-action/index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Executes all action steps in order.<br>[Kraken API]<br>open fun [execute](execute.md)(action: [SimulationDecisionAdapter.ExecutableAction](-executable-action/index.md), allowedStepTypes: [Set](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/Set.html)&lt;[SimulationDecisionAdapter.ExecutableStepType](-executable-step-type/index.md)&gt;): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Executes all action steps in order, filtered by allowed step types. |
