//[lib](../../../index.md)/[com.kraken.api.query.container.bank](../index.md)/[BankEntity](index.md)/[withdraw](withdraw.md)

# withdraw

[Kraken API]\
open fun [withdraw](withdraw.md)(amount: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Withdraws a specific amount. Handles &quot;Withdraw-X&quot; logic including scripts. Defaults to un-noted (Item) mode.

#### Return

true if the withdrawal was successful and false otherwise

#### Parameters

Kraken API

| | |
|---|---|
| amount | The amount of the item to withdraw |

[Kraken API]\
open fun [withdraw](withdraw.md)(amount: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), noted: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Withdraws a specific amount with explicit Note mode selection.

#### Return

true if the withdrawal was successful and false otherwise

#### Parameters

Kraken API

| | |
|---|---|
| amount | The amount of the item to withdraw: 1, 5, or 10. Any other value will withdraw X of that value |
| noted | True if the items should be withdrawn as notes and false if they should be withdrawn as items |
