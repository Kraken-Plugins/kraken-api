//[lib](../../../index.md)/[com.kraken.api.query.container.inventory](../index.md)/[InventoryEntity](index.md)

# InventoryEntity

[Kraken API]\
open class [InventoryEntity](index.md) : [AbstractEntity](../../com.kraken.api.core/-abstract-entity/index.md)&lt;[T](../../com.kraken.api.core/-abstract-entity/index.md)&gt;

## Constructors

| | |
|---|---|
| [InventoryEntity](-inventory-entity.md) | [Kraken API]<br>constructor(ctx: [Context](../../com.kraken.api/-context/index.md), raw: [ContainerItem](../../com.kraken.api.query.container/-container-item/index.md)) |

## Functions

| Name | Summary |
|---|---|
| [combineWith](combine-with.md) | [Kraken API]<br>open fun [combineWith](combine-with.md)(other: [ContainerItem](../../com.kraken.api.query.container/-container-item/index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Uses one inventory item on another. |
| [drop](drop.md) | [Kraken API]<br>open fun [drop](drop.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Drops the item from the inventory. |
| [equals](../../com.kraken.api.core/-abstract-entity/equals.md) | [Kraken API]<br>open fun [equals](../../com.kraken.api.core/-abstract-entity/equals.md)(o: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [getId](get-id.md) | [Kraken API]<br>open fun [getId](get-id.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)<br>The item ID for the wrapped game entity |
| [getName](get-name.md) | [Kraken API]<br>open fun [getName](get-name.md)(): [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)<br>The game entities name. |
| [hasAction](has-action.md) | [Kraken API]<br>open fun [hasAction](has-action.md)(action: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Returns true if the inventory item has the specified action. |
| [hashCode](../../com.kraken.api.core/-abstract-entity/hash-code.md) | [Kraken API]<br>open fun [hashCode](../../com.kraken.api.core/-abstract-entity/hash-code.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [interact](interact.md) | [Kraken API]<br>open fun [interact](interact.md)(action: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Interacts with the entity using the given action verb. |
| [isNull](../../com.kraken.api.core/-abstract-entity/is-null.md) | [Kraken API]<br>open fun [isNull](../../com.kraken.api.core/-abstract-entity/is-null.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>True when the game entity is null and false otherwise. |
| [raw](../../com.kraken.api.core/-abstract-entity/raw.md) | [Kraken API]<br>open fun [raw](../../com.kraken.api.core/-abstract-entity/raw.md)(): [T](../../com.kraken.api.core/-abstract-entity/index.md)<br>Returns the wrapped (raw) RuneLite API object for this interactable game entity. |
| [useOn](use-on.md) | [Kraken API]<br>open fun [useOn](use-on.md)(gameObject: GameObject): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Uses one item in the inventory on a Game object.<br>[Kraken API]<br>open fun [useOn](use-on.md)(npc: NPC): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Uses one item in the inventory on an NPC.<br>[Kraken API]<br>open fun [useOn](use-on.md)(other: [ContainerItem](../../com.kraken.api.query.container/-container-item/index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Uses one item in the inventory on the other. |
