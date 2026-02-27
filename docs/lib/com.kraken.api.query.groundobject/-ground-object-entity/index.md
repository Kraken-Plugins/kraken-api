//[lib](../../../index.md)/[com.kraken.api.query.groundobject](../index.md)/[GroundObjectEntity](index.md)

# GroundObjectEntity

[Kraken API]\
open class [GroundObjectEntity](index.md) : [AbstractEntity](../../com.kraken.api.core/-abstract-entity/index.md)&lt;[T](../../com.kraken.api.core/-abstract-entity/index.md)&gt;

## Constructors

| | |
|---|---|
| [GroundObjectEntity](-ground-object-entity.md) | [Kraken API]<br>constructor(ctx: [Context](../../com.kraken.api/-context/index.md), raw: [GroundItem](../-ground-item/index.md)) |

## Functions

| Name | Summary |
|---|---|
| [equals](../../com.kraken.api.core/-abstract-entity/equals.md) | [Kraken API]<br>open fun [equals](../../com.kraken.api.core/-abstract-entity/equals.md)(o: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [getId](get-id.md) | [Kraken API]<br>open fun [getId](get-id.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)<br>The item ID for the wrapped game entity |
| [getName](get-name.md) | [Kraken API]<br>open fun [getName](get-name.md)(): [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)<br>The game entities name. |
| [hashCode](../../com.kraken.api.core/-abstract-entity/hash-code.md) | [Kraken API]<br>open fun [hashCode](../../com.kraken.api.core/-abstract-entity/hash-code.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [interact](interact.md) | [Kraken API]<br>open fun [interact](interact.md)(action: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Interact interacts with the ground object. |
| [isInArea](is-in-area.md) | [Kraken API]<br>open fun [isInArea](is-in-area.md)(area: [GameArea](../../com.kraken.api.service.tile/-game-area/index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Checks if the ground object is within the game area. |
| [isNull](../../com.kraken.api.core/-abstract-entity/is-null.md) | [Kraken API]<br>open fun [isNull](../../com.kraken.api.core/-abstract-entity/is-null.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>True when the game entity is null and false otherwise. |
| [raw](../../com.kraken.api.core/-abstract-entity/raw.md) | [Kraken API]<br>open fun [raw](../../com.kraken.api.core/-abstract-entity/raw.md)(): [T](../../com.kraken.api.core/-abstract-entity/index.md)<br>Returns the wrapped (raw) RuneLite API object for this interactable game entity. |
| [take](take.md) | [Kraken API]<br>open fun [take](take.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Takes an item from the ground to be placed in the players inventory. |
