//[lib](../../../index.md)/[com.kraken.api.query.container](../index.md)/[ContainerItem](index.md)

# ContainerItem

[Kraken API]\
open class [ContainerItem](index.md)

Represents an item stored in an item container (either the inventory or Bank).

## Constructors

| | |
|---|---|
| [ContainerItem](-container-item.md) | [Kraken API]<br>constructor(item: Item, itemComposition: ItemComposition, slot: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), context: [Context](../../com.kraken.api/-context/index.md), widget: Widget, origin: [ContainerItem.ItemOrigin](-item-origin/index.md)) |

## Types

| Name | Summary |
|---|---|
| [ItemOrigin](-item-origin/index.md) | [Kraken API]<br>enum [ItemOrigin](-item-origin/index.md) |

## Properties

| Name | Summary |
|---|---|
| [equipmentActions](equipment-actions.md) | [Kraken API]<br>open val [equipmentActions](equipment-actions.md): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;[String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)&gt; |
| [inventoryActions](inventory-actions.md) | [Kraken API]<br>open val [inventoryActions](inventory-actions.md): [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)&gt; |
| [isNoted](is-noted.md) | [Kraken API]<br>open val [isNoted](is-noted.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [isStackable](is-stackable.md) | [Kraken API]<br>open val [isStackable](is-stackable.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [isTradeable](is-tradeable.md) | [Kraken API]<br>open val [isTradeable](is-tradeable.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [itemComposition](item-composition.md) | [Kraken API]<br>open val [itemComposition](item-composition.md): ItemComposition |
| [name](name.md) | [Kraken API]<br>open val [name](name.md): [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html) |

## Functions

| Name | Summary |
|---|---|
| [equals](equals.md) | [Kraken API]<br>open fun [equals](equals.md)(obj: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [getBounds](get-bounds.md) | [Kraken API]<br>open fun [getBounds](get-bounds.md)(context: [Context](../../com.kraken.api/-context/index.md), client: Client): [Rectangle](https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/java/awt/Rectangle.html)<br>Returns the rectangle bounds of an inventory item |
| [getHaPrice](get-ha-price.md) | [Kraken API]<br>open fun [getHaPrice](get-ha-price.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)<br>The high alchemy price for the item |
| [hashCode](hash-code.md) | [Kraken API]<br>open fun [hashCode](hash-code.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [isFood](is-food.md) | [Kraken API]<br>open fun [isFood](is-food.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>True if the item is food. |
| [toString](to-string.md) | [Kraken API]<br>open fun [toString](to-string.md)(): [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html) |
