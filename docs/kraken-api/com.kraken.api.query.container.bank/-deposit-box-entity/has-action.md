//[kraken-api](../../../index.md)/[com.kraken.api.query.container.bank](../index.md)/[DepositBoxEntity](index.md)/[hasAction](has-action.md)

# hasAction

[Kraken API]\
open fun [hasAction](has-action.md)(action: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Checks if the specified action is available in the inventory actions of the container item. 

This method retrieves the item's inventory actions and verifies if any action matches the provided action string, ignoring case sensitivity.

#### Return

`true` if the specified action is available in the item's inventory actions; `false` otherwise.

#### Parameters

Kraken API

| | |
|---|---|
| action | a `String` representing the name of the action to check for. It is case-insensitive, and null or empty actions are ignored. |
