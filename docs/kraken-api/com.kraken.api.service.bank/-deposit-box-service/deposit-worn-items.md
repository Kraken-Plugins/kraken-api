//[kraken-api](../../../index.md)/[com.kraken.api.service.bank](../index.md)/[DepositBoxService](index.md)/[depositWornItems](deposit-worn-items.md)

# depositWornItems

[Kraken API]\
open fun [depositWornItems](deposit-worn-items.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Deposits all worn items from the player's equipment into the bank deposit box. 

This method interacts with the deposit box interface widget in the game client to move all items currently equipped by the player into the bank. It uses the @ctxProvider to access the relevant widget component and send the interaction command.

The method assumes that the deposit box interface is already open and accessible for interaction. If the interface is not open, the operation may fail.

The widget interaction is performed using the @BankDepositbox.DEPOSIT_WORN constant and invokes the interaction index 1 for depositing worn items.

#### Return

`true` if the interaction was successfully executed and worn items were deposited; `false` otherwise if the interaction fails or conditions are not met.
