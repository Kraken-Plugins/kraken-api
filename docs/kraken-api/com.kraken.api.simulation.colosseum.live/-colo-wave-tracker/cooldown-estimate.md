//[kraken-api](../../../index.md)/[com.kraken.api.simulation.colosseum.live](../index.md)/[ColoWaveTracker](index.md)/[cooldownEstimate](cooldown-estimate.md)

# cooldownEstimate

[Kraken API]\
open fun [cooldownEstimate](cooldown-estimate.md)(record: [ColoWaveTracker.NpcRecord](-npc-record/index.md)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)

Estimates an NPC's current attack cooldown from its last observed attack.

#### Return

estimated ticks until the npc can act (0 when unknown - conservative).

#### Parameters

Kraken API

| | |
|---|---|
| record | npc record. |
