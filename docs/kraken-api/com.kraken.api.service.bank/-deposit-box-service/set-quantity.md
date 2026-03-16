//[kraken-api](../../../index.md)/[com.kraken.api.service.bank](../index.md)/[DepositBoxService](index.md)/[setQuantity](set-quantity.md)

# setQuantity

[Kraken API]\
open fun [setQuantity](set-quantity.md)(amount: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Sets the deposit quantity in the bank deposit box interface to the specified amount. 

This method interacts with the game client's deposit box interface to set the quantity of items to be deposited or withdrawn. It supports predefined options such as `1`, `5`, `10`, and `ALL`, as well as a custom quantity input via numerical dialogue for other values.

- 
   `1`: Sets the quantity to deposit or withdraw 1 item.
- 
   `5`: Sets the quantity to deposit or withdraw 5 items.
- 
   `10`: Sets the quantity to deposit or withdraw 10 items.
- 
   `ALL`: Deposits or withdraws all items.
- Other values: Prompts a numeric input dialogue to specify the desired quantity.

If the specified amount is not among the predefined options, the method invokes a dialogue service to continue with a custom quantity input.

#### Return

`true` if the operation to set the quantity or initiate the numerical dialogue is successful; `false` otherwise.

#### Parameters

Kraken API

| | |
|---|---|
| amount | the quantity to set in the deposit box interface. Acceptable values are `1`, `5`, `10`, `-1` (for `ALL`), or any positive integer for custom input. |
