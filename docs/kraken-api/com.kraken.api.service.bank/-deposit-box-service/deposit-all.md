//[kraken-api](../../../index.md)/[com.kraken.api.service.bank](../index.md)/[DepositBoxService](index.md)/[depositAll](deposit-all.md)

# depositAll

[Kraken API]\
open fun [depositAll](deposit-all.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Deposits all items from the player's inventory into the bank deposit box. 

This method interacts with the deposit box interface in the game client to move all inventory items into the bank. It utilizes the @ctxProvider to access the relevant game widgets and send the interaction command.

The method assumes that the deposit box interface is already open and accessible for interaction. If the interface is not open, the operation may fail.

#### Return

`true` if the interaction with the deposit box to deposit all items is successful; `false` otherwise.
