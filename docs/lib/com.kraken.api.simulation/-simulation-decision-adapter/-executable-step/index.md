//[lib](../../../../index.md)/[com.kraken.api.simulation](../../index.md)/[SimulationDecisionAdapter](../index.md)/[ExecutableStep](index.md)

# ExecutableStep

[Kraken API]\
class [ExecutableStep](index.md)

Runtime step payload translated from simulation decisions.

## Constructors

| | |
|---|---|
| [ExecutableStep](-executable-step.md) | [Kraken API]<br>constructor() |

## Functions

| Name | Summary |
|---|---|
| [castSpell](cast-spell.md) | [Kraken API]<br>open fun [castSpell](cast-spell.md)(spell: [CastableSpell](../../../com.kraken.api.service.magic/-castable-spell/index.md), targetNpcIndex: [Integer](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Integer.html)): [SimulationDecisionAdapter.ExecutableStep](index.md)<br>Creates spell cast step. |
| [equipItem](equip-item.md) | [Kraken API]<br>open fun [equipItem](equip-item.md)(itemId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [SimulationDecisionAdapter.ExecutableStep](index.md)<br>Creates equipment step. |
| [inventoryInteract](inventory-interact.md) | [Kraken API]<br>open fun [inventoryInteract](inventory-interact.md)(itemId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), action: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)): [SimulationDecisionAdapter.ExecutableStep](index.md)<br>Creates inventory interaction step. |
| [move](move.md) | [Kraken API]<br>open fun [move](move.md)(destination: WorldPoint): [SimulationDecisionAdapter.ExecutableStep](index.md)<br>Creates movement step. |
| [npcInteract](npc-interact.md) | [Kraken API]<br>open fun [npcInteract](npc-interact.md)(npcIndex: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), action: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)): [SimulationDecisionAdapter.ExecutableStep](index.md)<br>Creates npc interaction step. |
| [switchPrayer](switch-prayer.md) | [Kraken API]<br>open fun [switchPrayer](switch-prayer.md)(prayer: Prayer): [SimulationDecisionAdapter.ExecutableStep](index.md)<br>Creates prayer switch step. |
