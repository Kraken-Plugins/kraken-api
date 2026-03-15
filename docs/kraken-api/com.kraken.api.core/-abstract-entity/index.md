//[kraken-api](../../../index.md)/[com.kraken.api.core](../index.md)/[AbstractEntity](index.md)

# AbstractEntity

abstract class [AbstractEntity](index.md)&lt;[T](index.md)&gt; : [Interactable](../-interactable/index.md)&lt;[T](../-interactable/index.md)&gt; 

#### Inheritors

| |
|---|
| [BankEntity](../../com.kraken.api.query.container.bank/-bank-entity/index.md) |
| [BankInventoryEntity](../../com.kraken.api.query.container.bank/-bank-inventory-entity/index.md) |
| [DepositBoxEntity](../../com.kraken.api.query.container.bank/-deposit-box-entity/index.md) |
| [InventoryEntity](../../com.kraken.api.query.container.inventory/-inventory-entity/index.md) |
| [EquipmentEntity](../../com.kraken.api.query.equipment/-equipment-entity/index.md) |
| [GameObjectEntity](../../com.kraken.api.query.gameobject/-game-object-entity/index.md) |
| [GroundObjectEntity](../../com.kraken.api.query.groundobject/-ground-object-entity/index.md) |
| [NpcEntity](../../com.kraken.api.query.npc/-npc-entity/index.md) |
| [PlayerEntity](../../com.kraken.api.query.player/-player-entity/index.md) |
| [WidgetEntity](../../com.kraken.api.query.widget/-widget-entity/index.md) |
| [WorldEntity](../../com.kraken.api.query.world/-world-entity/index.md) |

## Constructors

| | |
|---|---|
| [AbstractEntity](-abstract-entity.md) | [Kraken API]<br>constructor(ctx: [Context](../../com.kraken.api/-context/index.md), raw: [T](index.md)) |

## Functions

| Name | Summary |
|---|---|
| [equals](equals.md) | [Kraken API]<br>open fun [equals](equals.md)(o: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [getId](../-interactable/get-id.md) | [Kraken API]<br>abstract fun [getId](../-interactable/get-id.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)<br>The item ID for the wrapped game entity |
| [getName](../-interactable/get-name.md) | [Kraken API]<br>abstract fun [getName](../-interactable/get-name.md)(): [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)<br>The game entities name. |
| [hashCode](hash-code.md) | [Kraken API]<br>open fun [hashCode](hash-code.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [interact](../-interactable/interact.md) | [Kraken API]<br>abstract fun [interact](../-interactable/interact.md)(action: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Interacts with the entity using the given action verb. |
| [isNull](is-null.md) | [Kraken API]<br>open fun [isNull](is-null.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>True when the game entity is null and false otherwise. |
| [raw](raw.md) | [Kraken API]<br>open fun [raw](raw.md)(): [T](index.md)<br>Returns the wrapped (raw) RuneLite API object for this interactable game entity. |
