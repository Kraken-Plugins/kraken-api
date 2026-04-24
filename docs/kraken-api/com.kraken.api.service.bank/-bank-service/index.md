//[kraken-api](../../../index.md)/[com.kraken.api.service.bank](../index.md)/[BankService](index.md)

# BankService

[Kraken API]\
open class [BankService](index.md)

A service class for interacting with global bank operations. This class handles operations on the bank interface like: opening, closing, depositing all items, and depositing all equipment. 

 For depositing or withdrawing specific items use `BankInventoryQuery` and `BankQuery` respectively.

## Constructors

| | |
|---|---|
| [BankService](-bank-service.md) | [Kraken API]<br>constructor() |

## Functions

| Name | Summary |
|---|---|
| [close](close.md) | [Kraken API]<br>open fun [close](close.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Closes the bank interface if it is open. |
| [depositAll](deposit-all.md) | [Kraken API]<br>open fun [depositAll](deposit-all.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Deposit all items in the players inventory into the bank. |
| [depositAllEquipment](deposit-all-equipment.md) | [Kraken API]<br>open fun [depositAllEquipment](deposit-all-equipment.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Deposits all worn items from the players equipment tab into the bank. |
| [depositContainers](deposit-containers.md) | [Kraken API]<br>open fun [depositContainers](deposit-containers.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Deposits items in stored containers (like wilderness loot bags, rune pouches, etc...) into the bank. |
| [enterPin](enter-pin.md) | [Kraken API]<br>open fun [enterPin](enter-pin.md)(pin: [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)&gt;): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Enters the bank pin using the provided 4 digits. |
| [getXAmount](get-x-amount.md) | [Kraken API]<br>open fun [getXAmount](get-x-amount.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)<br>Returns the current amount the X variable is set to. |
| [isClosed](is-closed.md) | [Kraken API]<br>open fun [isClosed](is-closed.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Determines whether the bank interface is closed. |
| [isOpen](is-open.md) | [Kraken API]<br>open fun [isOpen](is-open.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Checks whether the bank interface is open. |
| [isPinOpen](is-pin-open.md) | [Kraken API]<br>open fun [isPinOpen](is-pin-open.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Returns true when the bank PIN interface is open and false otherwise. |
| [onScriptCallbackEvent](on-script-callback-event.md) | [Kraken API]<br>open fun [onScriptCallbackEvent](on-script-callback-event.md)(event: ScriptCallbackEvent) |
| [setItem](set-item.md) | [Kraken API]<br>open fun [setItem](set-item.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Sets the withdraw mode to withdraw as items instead of notes. |
| [setNoted](set-noted.md) | [Kraken API]<br>open fun [setNoted](set-noted.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Sets the withdraw mode to withdraw as notes instead of items. |
| [setWithdrawMode](set-withdraw-mode.md) | [Kraken API]<br>open fun [setWithdrawMode](set-withdraw-mode.md)(noted: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Sets the withdrawal mode as either a note or item. |
| [setXAmount](set-x-amount.md) | [Kraken API]<br>open fun [setXAmount](set-x-amount.md)(amount: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html))<br>Sets the X number of items to withdraw or deposit. |
