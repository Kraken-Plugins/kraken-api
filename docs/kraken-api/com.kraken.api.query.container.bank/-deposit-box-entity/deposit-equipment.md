//[kraken-api](../../../index.md)/[com.kraken.api.query.container.bank](../index.md)/[DepositBoxEntity](index.md)/[depositEquipment](deposit-equipment.md)

# depositEquipment

[Kraken API]\
open fun [depositEquipment](deposit-equipment.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Deposits the filtered equipped item from the player's inventory into the bank deposit box. This will only work when `ctx.depositBox().inEquipment();` is used to filter the deposit box. 

This method interacts with the game's widget system to send a deposit request for a single piece of equipment.

#### Return

`true` if the deposit operation was executed successfully, `false` otherwise.
