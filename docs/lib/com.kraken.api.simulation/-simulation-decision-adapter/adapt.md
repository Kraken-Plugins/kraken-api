//[lib](../../../index.md)/[com.kraken.api.simulation](../index.md)/[SimulationDecisionAdapter](index.md)/[adapt](adapt.md)

# adapt

[Kraken API]\
open fun [adapt](adapt.md)(result: [DecisionTreeSearch.Result](../../com.kraken.api.simulation.tree/-decision-tree-search/-result/index.md), rootState: [SimulationState](../-simulation-state/index.md)): [SimulationDecisionAdapter.ExecutableAction](-executable-action/index.md)

Converts a decision result into an executable action with no optional interaction step.

#### Return

executable action.

#### Parameters

Kraken API

| | |
|---|---|
| result | decision tree search result. |
| rootState | root simulation state. |

[Kraken API]\
open fun [adapt](adapt.md)(result: [DecisionTreeSearch.Result](../../com.kraken.api.simulation.tree/-decision-tree-search/-result/index.md), rootState: [SimulationState](../-simulation-state/index.md), interactionAction: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html), interactionDistance: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [SimulationDecisionAdapter.ExecutableAction](-executable-action/index.md)

Converts a decision result into an executable action with optional npc interaction.

#### Return

executable action.

#### Parameters

Kraken API

| | |
|---|---|
| result | decision tree result. |
| rootState | root simulation state. |
| interactionAction | npc action to execute (for example &quot;Attack&quot;), null/empty disables interactions. |
| interactionDistance | Chebyshev distance for selecting an interaction target. |

[Kraken API]\
open fun [adapt](adapt.md)(result: [DecisionTreeSearch.Result](../../com.kraken.api.simulation.tree/-decision-tree-search/-result/index.md), rootState: [SimulationState](../-simulation-state/index.md), options: [SimulationDecisionAdapter.AdaptOptions](-adapt-options/index.md)): [SimulationDecisionAdapter.ExecutableAction](-executable-action/index.md)

Converts a decision result into an executable action with configurable adaptation options.

#### Return

executable action.

#### Parameters

Kraken API

| | |
|---|---|
| result | decision tree result. |
| rootState | root simulation state. |
| options | action adaptation options. |
