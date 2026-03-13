//[kraken-api](../../../index.md)/[com.kraken.api.simulation](../index.md)/[SimulationScenario](index.md)/[SimulationScenario](-simulation-scenario.md)

# SimulationScenario

[Kraken API]\
constructor(snapshot: [SimulationSnapshot](../../com.kraken.api.simulation.snapshot/-simulation-snapshot/index.md), npcProfilesById: [Map](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/Map.html)&lt;[Integer](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Integer.html), [SimulationNpcProfile](../-simulation-npc-profile/index.md)&gt;)

Creates a scenario from a snapshot and npc-id profile mapping.

#### Parameters

Kraken API

| | |
|---|---|
| snapshot | immutable snapshot of player, npcs, and collision. |
| npcProfilesById | mapping keyed by npc id. |

[Kraken API]\
constructor(snapshot: [SimulationSnapshot](../../com.kraken.api.simulation.snapshot/-simulation-snapshot/index.md), npcProfilesById: [Map](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/Map.html)&lt;[Integer](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Integer.html), [SimulationNpcProfile](../-simulation-npc-profile/index.md)&gt;, defaultNpcProfile: [SimulationNpcProfile](../-simulation-npc-profile/index.md))

Creates a scenario from a snapshot and npc-id profile mapping.

#### Parameters

Kraken API

| | |
|---|---|
| snapshot | immutable snapshot of player, npcs, and collision. |
| npcProfilesById | mapping keyed by npc id. |
| defaultNpcProfile | fallback profile when a mapping is missing. |
