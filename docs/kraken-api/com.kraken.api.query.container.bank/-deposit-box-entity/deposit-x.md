//[kraken-api](../../../index.md)/[com.kraken.api.query.container.bank](../index.md)/[DepositBoxEntity](index.md)/[depositX](deposit-x.md)

# depositX

[Kraken API]\
open fun [depositX](deposit-x.md)(amount: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Deposits X amount of an item from the players inventory into the bank deposit box. This will **NOT** work or depositing equipment if `ctx.depositBox().inEquipment();` is used to filter the deposit box.

#### Return

true if the deposit was successful and false otherwise.

#### Parameters

Kraken API

| | |
|---|---|
| amount | The amount of the item to deposit. |
