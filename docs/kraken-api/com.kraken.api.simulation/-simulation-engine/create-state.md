//[kraken-api](../../../index.md)/[com.kraken.api.simulation](../index.md)/[SimulationEngine](index.md)/[createState](create-state.md)

# createState

[Kraken API]\
open fun [createState](create-state.md)(scenario: [SimulationScenario](../-simulation-scenario/index.md)): [SimulationState](../-simulation-state/index.md)

Creates a root state.

#### Return

mutable state.

#### Parameters

Kraken API

| | |
|---|---|
| scenario | simulation scenario. |

[Kraken API]\
open fun [createState](create-state.md)(snapshot: [SimulationSnapshot](../../com.kraken.api.simulation.snapshot/-simulation-snapshot/index.md), npcProfilesById: [Map](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/Map.html)&lt;[Integer](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Integer.html), [SimulationNpcProfile](../-simulation-npc-profile/index.md)&gt;): [SimulationState](../-simulation-state/index.md)

Creates a root state.

#### Return

mutable state.

#### Parameters

Kraken API

| | |
|---|---|
| snapshot | snapshot input. |
| npcProfilesById | npc profile map. |
