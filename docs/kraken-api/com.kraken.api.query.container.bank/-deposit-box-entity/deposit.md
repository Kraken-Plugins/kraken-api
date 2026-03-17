//[kraken-api](../../../index.md)/[com.kraken.api.query.container.bank](../index.md)/[DepositBoxEntity](index.md)/[deposit](deposit.md)

# deposit

[Kraken API]\
open fun [deposit](deposit.md)(amount: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Deposits a set amount of the given item from the players' inventory to the bank. If the amount is not one of: 1, 5, or 10, then all the given items will be deposited by default.

#### Return

True if the deposit was successful and false otherwise

#### Parameters

Kraken API

| | |
|---|---|
| amount | The amount of the item to deposit: 1, 5, 10, or any other integer for all the item |

[Kraken API]\
open fun [deposit](deposit.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Deposits a single instance of the specified item into the bank deposit box. If the instance is stackable like arrows it will deposit all arrows into the bank. 

 This method performs different actions based on the origin of the container item: 

- If the item's origin is @EQUIPMENT, it interacts with the associated widget to deposit the item.
- Otherwise, it uses the interaction manager to execute a &quot;Deposit-1&quot; action for the item.

#### Return

`true` if the deposit operation is executed successfully, `false` otherwise.
