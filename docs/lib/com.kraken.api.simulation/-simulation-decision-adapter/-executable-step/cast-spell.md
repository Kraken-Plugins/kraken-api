//[lib](../../../../index.md)/[com.kraken.api.simulation](../../index.md)/[SimulationDecisionAdapter](../index.md)/[ExecutableStep](index.md)/[castSpell](cast-spell.md)

# castSpell

[Kraken API]\
open fun [castSpell](cast-spell.md)(spell: [CastableSpell](../../../com.kraken.api.service.magic/-castable-spell/index.md), targetNpcIndex: [Integer](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Integer.html)): [SimulationDecisionAdapter.ExecutableStep](index.md)

Creates spell cast step.

#### Return

executable spell-cast step.

#### Parameters

Kraken API

| | |
|---|---|
| spell | spell to cast. |
| targetNpcIndex | optional target npc index, null for untargeted cast. |
