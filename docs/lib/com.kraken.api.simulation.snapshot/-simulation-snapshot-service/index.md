//[lib](../../../index.md)/[com.kraken.api.simulation.snapshot](../index.md)/[SimulationSnapshotService](index.md)

# SimulationSnapshotService

[Kraken API]\
class [SimulationSnapshotService](index.md)

Captures immutable snapshots from live RuneLite state.

## Constructors

| | |
|---|---|
| [SimulationSnapshotService](-simulation-snapshot-service.md) | [Kraken API]<br>constructor() |

## Types

| Name | Summary |
|---|---|
| [CaptureOptions](-capture-options/index.md) | [Kraken API]<br>class [CaptureOptions](-capture-options/index.md)<br>Capture options. |

## Functions

| Name | Summary |
|---|---|
| [capture](capture.md) | [Kraken API]<br>open fun [capture](capture.md)(): [SimulationSnapshot](../-simulation-snapshot/index.md)<br>Captures with defaults.<br>[Kraken API]<br>open fun [capture](capture.md)(options: [SimulationSnapshotService.CaptureOptions](-capture-options/index.md)): [SimulationSnapshot](../-simulation-snapshot/index.md)<br>Captures with explicit options.<br>[Kraken API]<br>open fun [capture](capture.md)(npcRadius: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [SimulationSnapshot](../-simulation-snapshot/index.md)<br>Captures with explicit npc radius. |
