//[kraken-api](../../../index.md)/[com.kraken.api.simulation](../index.md)/[SimulationScenario](index.md)

# SimulationScenario

[Kraken API]\
class [SimulationScenario](index.md)

Input bundle for simulation tree generation.

## Constructors

| | |
|---|---|
| [SimulationScenario](-simulation-scenario.md) | [Kraken API]<br>constructor(snapshot: [SimulationSnapshot](../../com.kraken.api.simulation.snapshot/-simulation-snapshot/index.md), npcProfilesById: [Map](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/Map.html)&lt;[Integer](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Integer.html), [SimulationNpcProfile](../-simulation-npc-profile/index.md)&gt;)<br>Creates a scenario from a snapshot and npc-id profile mapping.<br>constructor(snapshot: [SimulationSnapshot](../../com.kraken.api.simulation.snapshot/-simulation-snapshot/index.md), npcProfilesById: [Map](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/Map.html)&lt;[Integer](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Integer.html), [SimulationNpcProfile](../-simulation-npc-profile/index.md)&gt;, defaultNpcProfile: [SimulationNpcProfile](../-simulation-npc-profile/index.md))<br>Creates a scenario from a snapshot and npc-id profile mapping. |

## Functions

| Name | Summary |
|---|---|
| [resolveNpcProfile](resolve-npc-profile.md) | [Kraken API]<br>open fun [resolveNpcProfile](resolve-npc-profile.md)(npcId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [SimulationNpcProfile](../-simulation-npc-profile/index.md)<br>Resolves an NPC profile for an npc id. |
