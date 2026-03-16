//[kraken-api](../../../index.md)/[com.kraken.api.service.bank](../index.md)/[DepositBoxService](index.md)

# DepositBoxService

[Kraken API]\
open class [DepositBoxService](index.md)

Service class for managing interactions with the bank deposit box interface within the client. 

The `DepositBoxService` provides various methods to interact with the deposit box interface, allowing users to manage their inventory, worn items, and other specific interactions associated with the deposit functionality.

All interactions assume the deposit box interface is open from the client's context. Failing to meet this prerequisite may result in operation failure.

### Key Features:

- Checking the state of the deposit box interface (open/closed).
- Depositing all inventory items into the bank deposit box.
- Depositing worn items or specific worn equipment based on slot, item ID, or name.
- Depositing items from the looting bag into the deposit box.

The service utilizes a context provider (@ctxProvider) to locate relevant widgets and execute interaction commands within the game client. Proper widget interaction ensures seamless deposit operations and the appropriate state handling for the deposit box interface.

## Constructors

| | |
|---|---|
| [DepositBoxService](-deposit-box-service.md) | [Kraken API]<br>constructor() |

## Functions

| Name | Summary |
|---|---|
| [close](close.md) | [Kraken API]<br>open fun [close](close.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Closes the bank deposit box interface if it is currently open. |
| [depositAll](deposit-all.md) | [Kraken API]<br>open fun [depositAll](deposit-all.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Deposits all items from the player's inventory into the bank deposit box. |
| [depositLootingBag](deposit-looting-bag.md) | [Kraken API]<br>open fun [depositLootingBag](deposit-looting-bag.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Deposits all items from the looting bag into the bank deposit box. |
| [depositWorn](deposit-worn.md) | [Kraken API]<br>open fun [depositWorn](deposit-worn.md)(slot: EquipmentInventorySlot): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Deposits a specific worn equipment item from the player's inventory into the bank deposit box.<br>[Kraken API]<br>open fun [depositWorn](deposit-worn.md)(id: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Deposits a specific worn equipment item into the bank deposit box based on the item ID.<br>[Kraken API]<br>open fun [depositWorn](deposit-worn.md)(name: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Deposits an item currently equipped on the player into a storage system. |
| [depositWornItems](deposit-worn-items.md) | [Kraken API]<br>open fun [depositWornItems](deposit-worn-items.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Deposits all worn items from the player's equipment into the bank deposit box. |
| [getDepositBoxWidget](get-deposit-box-widget.md) | [Kraken API]<br>open fun [getDepositBoxWidget](get-deposit-box-widget.md)(slot: EquipmentInventorySlot): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)<br>Retrieves the interface widget ID corresponding to a deposit box slot for the specified EquipmentInventorySlot. |
| [isClosed](is-closed.md) | [Kraken API]<br>open fun [isClosed](is-closed.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Determines whether the bank deposit box interface is currently closed. |
| [isOpen](is-open.md) | [Kraken API]<br>open fun [isOpen](is-open.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Determines whether the bank deposit box interface is currently open. |
| [setQuantity](set-quantity.md) | [Kraken API]<br>open fun [setQuantity](set-quantity.md)(amount: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Sets the deposit quantity in the bank deposit box interface to the specified amount. |
