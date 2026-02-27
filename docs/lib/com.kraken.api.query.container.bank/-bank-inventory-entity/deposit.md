//[lib](../../../index.md)/[com.kraken.api.query.container.bank](../index.md)/[BankInventoryEntity](index.md)/[deposit](deposit.md)

# deposit

[Kraken API]\
open fun [deposit](deposit.md)(amount: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Deposits a set amount of the given item from the players inventory to the bank. If the amount is not one of: 1, 5, or 10 then all of the given item will be deposited by default.

#### Return

True if the deposit was successful and false otherwise

#### Parameters

Kraken API

| | |
|---|---|
| amount | The amount of the item to deposit: 1, 5, 10, or any other integer for all of the item |
