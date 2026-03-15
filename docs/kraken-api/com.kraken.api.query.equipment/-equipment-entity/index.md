//[kraken-api](../../../index.md)/[com.kraken.api.query.equipment](../index.md)/[EquipmentEntity](index.md)

# EquipmentEntity

[Kraken API]\
open class [EquipmentEntity](index.md) : [AbstractEntity](../../com.kraken.api.core/-abstract-entity/index.md)&lt;[T](../../com.kraken.api.core/-abstract-entity/index.md)&gt;

## Constructors

| | |
|---|---|
| [EquipmentEntity](-equipment-entity.md) | [Kraken API]<br>constructor(ctx: [Context](../../com.kraken.api/-context/index.md), raw: [ContainerItem](../../com.kraken.api.query.container/-container-item/index.md)) |

## Functions

| Name | Summary |
|---|---|
| [equals](../../com.kraken.api.core/-abstract-entity/equals.md) | [Kraken API]<br>open fun [equals](../../com.kraken.api.core/-abstract-entity/equals.md)(o: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [getId](get-id.md) | [Kraken API]<br>open fun [getId](get-id.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)<br>The item ID for the wrapped game entity |
| [getName](get-name.md) | [Kraken API]<br>open fun [getName](get-name.md)(): [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)<br>The game entities name. |
| [getSlot](get-slot.md) | [Kraken API]<br>open fun [getSlot](get-slot.md)(): EquipmentInventorySlot<br>Gets the slot this piece of equipment is in. |
| [hashCode](../../com.kraken.api.core/-abstract-entity/hash-code.md) | [Kraken API]<br>open fun [hashCode](../../com.kraken.api.core/-abstract-entity/hash-code.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [interact](interact.md) | [Kraken API]<br>open fun [interact](interact.md)(action: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Interacts with the entity using the given action verb. |
| [isNull](../../com.kraken.api.core/-abstract-entity/is-null.md) | [Kraken API]<br>open fun [isNull](../../com.kraken.api.core/-abstract-entity/is-null.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>True when the game entity is null and false otherwise. |
| [raw](../../com.kraken.api.core/-abstract-entity/raw.md) | [Kraken API]<br>open fun [raw](../../com.kraken.api.core/-abstract-entity/raw.md)(): [T](../../com.kraken.api.core/-abstract-entity/index.md)<br>Returns the wrapped (raw) RuneLite API object for this interactable game entity. |
| [remove](remove.md) | [Kraken API]<br>open fun [remove](remove.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Removes an equipped weapon or piece of armour. |
| [wear](wear.md) | [Kraken API]<br>open fun [wear](wear.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Wears an equippable item |
| [wield](wield.md) | [Kraken API]<br>open fun [wield](wield.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Wields a weapon. |
| [wieldOrWear](wield-or-wear.md) | [Kraken API]<br>open fun [wieldOrWear](wield-or-wear.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Wields or wears an equippable item dependent on the item. |
