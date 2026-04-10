//[kraken-api](../../../index.md)/[com.kraken.api.service.ui.processing](../index.md)/[ProcessingService](index.md)

# ProcessingService

[Kraken API]\
open class [ProcessingService](index.md)

## Constructors

| | |
|---|---|
| [ProcessingService](-processing-service.md) | [Kraken API]<br>constructor() |

## Functions

| Name | Summary |
|---|---|
| [getAmount](get-amount.md) | [Kraken API]<br>open fun [getAmount](get-amount.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)<br>Retrieves the current value of the skill multi-quantity variable. |
| [isOpen](is-open.md) | [Kraken API]<br>open fun [isOpen](is-open.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Determines whether the widget corresponding to the specified interface ID is currently open and visible. |
| [process](process.md) | [Kraken API]<br>open fun [process](process.md)(action: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html), containerItem: [ContainerItem](../../com.kraken.api.query.container/-container-item/index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Confirms the selection of one of the specified container items by resuming the appropriate widget interaction based on the current multi-quantity value.<br>[Kraken API]<br>open fun [process](process.md)(action: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html), itemIds: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)&gt;): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Confirms the selection of one of the specified item IDs by resuming the appropriate widget interaction based on the current multi-quantity value.<br>[Kraken API]<br>open fun [process](process.md)(action: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html), itemNames: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)&gt;): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Confirms the selection of one of the specified item names by resuming the appropriate widget interaction based on the current multi-quantity value. |
| [processByIndex](process-by-index.md) | [Kraken API]<br>open fun [processByIndex](process-by-index.md)(action: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html), index: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html))<br>Confirms the selected index by resuming the specific widget and child interface associated with the provided index and the current multi-quantity value. |
| [setAmount](set-amount.md) | [Kraken API]<br>open fun [setAmount](set-amount.md)(amount: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html))<br>Sets the skill multi-quantity value to the specified amount. |
