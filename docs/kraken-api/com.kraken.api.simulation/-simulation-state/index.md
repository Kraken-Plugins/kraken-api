//[kraken-api](../../../index.md)/[com.kraken.api.simulation](../index.md)/[SimulationState](index.md)

# SimulationState

[Kraken API]\
class [SimulationState](index.md)

Mutable branchable state used while expanding the simulation tree.

## Properties

| Name | Summary |
|---|---|
| [equippedItemIds](equipped-item-ids.md) | [Kraken API]<br>val [equippedItemIds](equipped-item-ids.md): [Set](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/Set.html)&lt;[Integer](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Integer.html)&gt; |
| [inventoryItemCounts](inventory-item-counts.md) | [Kraken API]<br>val [inventoryItemCounts](inventory-item-counts.md): [Map](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/Map.html)&lt;[Integer](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Integer.html), [Integer](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Integer.html)&gt; |

## Functions

| Name | Summary |
|---|---|
| [addInventoryItem](add-inventory-item.md) | [Kraken API]<br>open fun [addInventoryItem](add-inventory-item.md)(itemId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html))<br>Adds an item to inventory counts. |
| [consumeInventoryItem](consume-inventory-item.md) | [Kraken API]<br>open fun [consumeInventoryItem](consume-inventory-item.md)(itemId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Consumes one inventory item. |
| [copy](copy.md) | [Kraken API]<br>open fun [copy](copy.md)(): [SimulationState](index.md)<br>Creates a branch-safe copy. |
| [damagePlayer](damage-player.md) | [Kraken API]<br>open fun [damagePlayer](damage-player.md)(amount: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html))<br>Applies damage to player hitpoints. |
| [equipItemFromInventory](equip-item-from-inventory.md) | [Kraken API]<br>open fun [equipItemFromInventory](equip-item-from-inventory.md)(itemId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Equips an item from inventory. |
| [findNpcSlotByIndex](find-npc-slot-by-index.md) | [Kraken API]<br>open fun [findNpcSlotByIndex](find-npc-slot-by-index.md)(npcIndex: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)<br>Finds a simulation npc slot from RuneLite npc index. |
| [fromScenario](from-scenario.md) | [Kraken API]<br>open fun [fromScenario](from-scenario.md)(scenario: [SimulationScenario](../-simulation-scenario/index.md)): [SimulationState](index.md) |
| [getInventoryItemCount](get-inventory-item-count.md) | [Kraken API]<br>open fun [getInventoryItemCount](get-inventory-item-count.md)(itemId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [getNpcAttackCooldown](get-npc-attack-cooldown.md) | [Kraken API]<br>open fun [getNpcAttackCooldown](get-npc-attack-cooldown.md)(npcSlot: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [getNpcId](get-npc-id.md) | [Kraken API]<br>open fun [getNpcId](get-npc-id.md)(npcSlot: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [getNpcIndex](get-npc-index.md) | [Kraken API]<br>open fun [getNpcIndex](get-npc-index.md)(npcSlot: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [getNpcPackedPoint](get-npc-packed-point.md) | [Kraken API]<br>open fun [getNpcPackedPoint](get-npc-packed-point.md)(npcSlot: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [getNpcProfile](get-npc-profile.md) | [Kraken API]<br>open fun [getNpcProfile](get-npc-profile.md)(npcSlot: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [SimulationNpcProfile](../-simulation-npc-profile/index.md) |
| [getNpcSize](get-npc-size.md) | [Kraken API]<br>open fun [getNpcSize](get-npc-size.md)(npcSlot: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [getNpcWorldPoint](get-npc-world-point.md) | [Kraken API]<br>open fun [getNpcWorldPoint](get-npc-world-point.md)(npcSlot: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): WorldPoint |
| [getNpcX](get-npc-x.md) | [Kraken API]<br>open fun [getNpcX](get-npc-x.md)(npcSlot: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [getNpcY](get-npc-y.md) | [Kraken API]<br>open fun [getNpcY](get-npc-y.md)(npcSlot: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [getPlayerWorldPoint](get-player-world-point.md) | [Kraken API]<br>open fun [getPlayerWorldPoint](get-player-world-point.md)(): WorldPoint |
| [getPlayerX](get-player-x.md) | [Kraken API]<br>open fun [getPlayerX](get-player-x.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [getPlayerY](get-player-y.md) | [Kraken API]<br>open fun [getPlayerY](get-player-y.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [getQueuedMovementDestination](get-queued-movement-destination.md) | [Kraken API]<br>open fun [getQueuedMovementDestination](get-queued-movement-destination.md)(): WorldPoint |
| [hasInventoryItem](has-inventory-item.md) | [Kraken API]<br>open fun [hasInventoryItem](has-inventory-item.md)(itemId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [hasQueuedMovement](has-queued-movement.md) | [Kraken API]<br>open fun [hasQueuedMovement](has-queued-movement.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [healPlayer](heal-player.md) | [Kraken API]<br>open fun [healPlayer](heal-player.md)(amount: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html))<br>Heals player hitpoints. |
| [isItemEquipped](is-item-equipped.md) | [Kraken API]<br>open fun [isItemEquipped](is-item-equipped.md)(itemId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [isNpcActive](is-npc-active.md) | [Kraken API]<br>open fun [isNpcActive](is-npc-active.md)(npcSlot: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [isQueuedMovementRun](is-queued-movement-run.md) | [Kraken API]<br>open fun [isQueuedMovementRun](is-queued-movement-run.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [setNpcActive](set-npc-active.md) | [Kraken API]<br>open fun [setNpcActive](set-npc-active.md)(npcSlot: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), active: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html))<br>Sets npc active status. |
| [setNpcAttackCooldown](set-npc-attack-cooldown.md) | [Kraken API]<br>open fun [setNpcAttackCooldown](set-npc-attack-cooldown.md)(npcSlot: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), cooldown: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html))<br>Sets npc attack cooldown. |
