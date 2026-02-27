//[lib](../../../index.md)/[com.kraken.api.query.container.bank](../index.md)/[BankQuery](index.md)/[isOpen](is-open.md)

# isOpen

[Kraken API]\
open fun [isOpen](is-open.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Determines whether the bank interface is currently open. 

This method interacts with the `BankService` to check the status of the bank interface. The bank is considered open if the corresponding interface is visible and active in the client.

#### Return

`true` if the bank interface is open, `false` otherwise.
