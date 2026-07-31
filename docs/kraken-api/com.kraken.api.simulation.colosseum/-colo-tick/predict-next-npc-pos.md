//[kraken-api](../../../index.md)/[com.kraken.api.simulation.colosseum](../index.md)/[ColoTick](index.md)/[predictNextNpcPos](predict-next-npc-pos.md)

# predictNextNpcPos

[Kraken API]\
open fun [predictNextNpcPos](predict-next-npc-pos.md)(s: [ColoState](../-colo-state/index.md), scratch: [ColoScratch](../-colo-scratch/index.md), slot: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Short](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-short/index.html)

Predicts the tile an NPC would step to this tick if it moved (ignoring wave-start gating and the has-line-of-sight hold). Used by the planner's prayer policy to anticipate attackers that step into line of sight and attack on the same tick.

#### Return

packed next anchor, or [NONE](../-colo-coords/-n-o-n-e.md) when the npc would not move.

#### Parameters

Kraken API

| | |
|---|---|
| s | state. |
| scratch | scratch memory (for route-finder approach fields). |
| slot | npc slot. |
