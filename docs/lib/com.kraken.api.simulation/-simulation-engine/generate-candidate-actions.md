//[lib](../../../index.md)/[com.kraken.api.simulation](../index.md)/[SimulationEngine](index.md)/[generateCandidateActions](generate-candidate-actions.md)

# generateCandidateActions

[Kraken API]\
open fun [generateCandidateActions](generate-candidate-actions.md)(state: [SimulationState](../-simulation-state/index.md), depthRemaining: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), options: [SimulationTreeOptions](../../com.kraken.api.simulation.tree/-simulation-tree-options/index.md), actionProvider: [SimulationEngine.ActionProvider](-action-provider/index.md)): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;[SimulationAction](../-simulation-action/index.md)&gt;

Generates legal candidate actions for a node.

#### Return

legal action list.

#### Parameters

Kraken API

| | |
|---|---|
| state | state input. |
| depthRemaining | depth remaining. |
| options | tree options. |
| actionProvider | optional provider. |
