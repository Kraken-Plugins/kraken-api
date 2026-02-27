//[lib](../../../index.md)/[com.kraken.api.simulation](../index.md)/[SimulationAction](index.md)

# SimulationAction

[Kraken API]\
class [SimulationAction](index.md)

Action edge used while expanding and executing simulation trees.

## Types

| Name | Summary |
|---|---|
| [Type](-type/index.md) | [Kraken API]<br>enum [Type](-type/index.md)<br>Simulation action kind. |

## Properties

| Name | Summary |
|---|---|
| [WAIT](-w-a-i-t.md) | [Kraken API]<br>val [WAIT](-w-a-i-t.md): [SimulationAction](index.md)<br>Canonical wait action. |

## Functions

| Name | Summary |
|---|---|
| [castSpell](cast-spell.md) | [Kraken API]<br>open fun [castSpell](cast-spell.md)(spell: [CastableSpell](../../com.kraken.api.service.magic/-castable-spell/index.md)): [SimulationAction](index.md)<br>Creates an untargeted spell cast. |
| [castSpellOnNpc](cast-spell-on-npc.md) | [Kraken API]<br>open fun [castSpellOnNpc](cast-spell-on-npc.md)(spell: [CastableSpell](../../com.kraken.api.service.magic/-castable-spell/index.md), targetNpcIndex: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [SimulationAction](index.md)<br>Creates a targeted spell cast. |
| [custom](custom.md) | [Kraken API]<br>open fun [custom](custom.md)(customActionId: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)): [SimulationAction](index.md)<br>Creates a custom action marker. |
| [equals](equals.md) | [Kraken API]<br>open fun [equals](equals.md)(other: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [equipItem](equip-item.md) | [Kraken API]<br>open fun [equipItem](equip-item.md)(itemId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [SimulationAction](index.md)<br>Creates an equip action. |
| [getMovementDestination](get-movement-destination.md) | [Kraken API]<br>open fun [getMovementDestination](get-movement-destination.md)(): WorldPoint<br>Resolves movement destination. |
| [hashCode](hash-code.md) | [Kraken API]<br>open fun [hashCode](hash-code.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [interactNpc](interact-npc.md) | [Kraken API]<br>open fun [interactNpc](interact-npc.md)(npcIndex: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), action: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)): [SimulationAction](index.md)<br>Creates an npc interaction action. |
| [isMovement](is-movement.md) | [Kraken API]<br>open fun [isMovement](is-movement.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [isWait](is-wait.md) | [Kraken API]<br>open fun [isWait](is-wait.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [moveTo](move-to.md) | [Kraken API]<br>open fun [moveTo](move-to.md)(destination: WorldPoint): [SimulationAction](index.md)<br>Creates a move action to a destination tile. |
| [moveToPacked](move-to-packed.md) | [Kraken API]<br>open fun [moveToPacked](move-to-packed.md)(packedWorldPoint: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), run: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)): [SimulationAction](index.md)<br>Creates a move action using a packed destination. |
| [runTo](run-to.md) | [Kraken API]<br>open fun [runTo](run-to.md)(destination: WorldPoint): [SimulationAction](index.md)<br>Creates a run action to a destination tile. |
| [switchPrayer](switch-prayer.md) | [Kraken API]<br>open fun [switchPrayer](switch-prayer.md)(prayer: Prayer): [SimulationAction](index.md)<br>Creates a prayer switch action. |
| [toString](to-string.md) | [Kraken API]<br>open fun [toString](to-string.md)(): [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html) |
