//[kraken-api](../../../index.md)/[com.kraken.api.query.container.bank](../index.md)/[DepositBoxEntity](index.md)/[depositTen](deposit-ten.md)

# depositTen

[Kraken API]\
open fun [depositTen](deposit-ten.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Deposits ten of the given items from the players' inventory into the bank deposit box. This will **NOT** work for depositing equipment if `ctx.depositBox().inEquipment();` is used to filter the deposit box.

#### Return

true if the deposit was successful and false otherwise.
