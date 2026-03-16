//[kraken-api](../../../index.md)/[com.kraken.api.query.container.bank](../index.md)/[DepositBoxEntity](index.md)

# DepositBoxEntity

[Kraken API]\
open class [DepositBoxEntity](index.md) : [AbstractEntity](../../com.kraken.api.core/-abstract-entity/index.md)&lt;[T](../../com.kraken.api.core/-abstract-entity/index.md)&gt;

## Constructors

| | |
|---|---|
| [DepositBoxEntity](-deposit-box-entity.md) | [Kraken API]<br>constructor(ctx: [Context](../../com.kraken.api/-context/index.md), raw: [ContainerItem](../../com.kraken.api.query.container/-container-item/index.md)) |

## Functions

| Name | Summary |
|---|---|
| [count](count.md) | [Kraken API]<br>open fun [count](count.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)<br>Returns the quantity of the item in your inventory when the bank deposit box interface is open. |
| [deposit](deposit.md) | [Kraken API]<br>open fun [deposit](deposit.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Deposits a single instance of the specified item into the bank deposit box.<br>[Kraken API]<br>open fun [deposit](deposit.md)(amount: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Deposits a set amount of the given item from the players' inventory to the bank. |
| [depositAll](deposit-all.md) | [Kraken API]<br>open fun [depositAll](deposit-all.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Deposits all the given items from the players inventory into the bank deposit box. |
| [depositFive](deposit-five.md) | [Kraken API]<br>open fun [depositFive](deposit-five.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Deposits five of the given items from the players' inventory into the bank deposit box. |
| [depositOne](deposit-one.md) | [Kraken API]<br>open fun [depositOne](deposit-one.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Deposits one of the given item from the players inventory into the bank deposit box. |
| [depositTen](deposit-ten.md) | [Kraken API]<br>open fun [depositTen](deposit-ten.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Deposits ten of the given items from the players' inventory into the bank deposit box. |
| [depositX](deposit-x.md) | [Kraken API]<br>open fun [depositX](deposit-x.md)(amount: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Deposits X amount of an item from the players inventory into the bank deposit box. |
| [equals](../../com.kraken.api.core/-abstract-entity/equals.md) | [Kraken API]<br>open fun [equals](../../com.kraken.api.core/-abstract-entity/equals.md)(o: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [getId](get-id.md) | [Kraken API]<br>open fun [getId](get-id.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)<br>The item ID for the wrapped game entity |
| [getName](get-name.md) | [Kraken API]<br>open fun [getName](get-name.md)(): [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)<br>The game entities name. |
| [hasAction](has-action.md) | [Kraken API]<br>open fun [hasAction](has-action.md)(action: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Checks if the specified action is available in the inventory actions of the container item. |
| [hashCode](../../com.kraken.api.core/-abstract-entity/hash-code.md) | [Kraken API]<br>open fun [hashCode](../../com.kraken.api.core/-abstract-entity/hash-code.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [interact](interact.md) | [Kraken API]<br>open fun [interact](interact.md)(action: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Interacts with the entity using the given action verb. |
| [isNull](../../com.kraken.api.core/-abstract-entity/is-null.md) | [Kraken API]<br>open fun [isNull](../../com.kraken.api.core/-abstract-entity/is-null.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>True when the game entity is null and false otherwise. |
| [raw](../../com.kraken.api.core/-abstract-entity/raw.md) | [Kraken API]<br>open fun [raw](../../com.kraken.api.core/-abstract-entity/raw.md)(): [T](../../com.kraken.api.core/-abstract-entity/index.md)<br>Returns the wrapped (raw) RuneLite API object for this interactable game entity. |
