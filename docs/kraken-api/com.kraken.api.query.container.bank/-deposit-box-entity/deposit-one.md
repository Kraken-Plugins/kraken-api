//[kraken-api](../../../index.md)/[com.kraken.api.query.container.bank](../index.md)/[DepositBoxEntity](index.md)/[depositOne](deposit-one.md)

# depositOne

[Kraken API]\
open fun [depositOne](deposit-one.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Deposits one of the given item from the players inventory into the bank deposit box. This will **NOT** work for depositing equipment if `ctx.depositBox().inEquipment();` is used to filter the deposit box.

#### Return

true if the deposit was successful and false otherwise.
