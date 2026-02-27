//[lib](../../../index.md)/[com.kraken.api.core](../index.md)/[Interactable](index.md)

# Interactable

interface [Interactable](index.md)&lt;[T](index.md)&gt;

#### Inheritors

| |
|---|
| [AbstractEntity](../-abstract-entity/index.md) |

## Functions

| Name | Summary |
|---|---|
| [getId](get-id.md) | [Kraken API]<br>abstract fun [getId](get-id.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)<br>The item ID for the wrapped game entity |
| [getName](get-name.md) | [Kraken API]<br>abstract fun [getName](get-name.md)(): [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)<br>The game entities name. |
| [interact](interact.md) | [Kraken API]<br>abstract fun [interact](interact.md)(action: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Interacts with the entity using the given action verb. |
| [isNull](is-null.md) | [Kraken API]<br>abstract fun [isNull](is-null.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>True when the game entity is null and false otherwise. |
| [raw](raw.md) | [Kraken API]<br>abstract fun [raw](raw.md)(): [T](index.md)<br>Returns the wrapped (raw) RuneLite API object for this interactable game entity. |
