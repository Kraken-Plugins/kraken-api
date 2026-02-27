//[lib](../../../index.md)/[com.kraken.api.service.bank](../index.md)/[BankService](index.md)/[setWithdrawMode](set-withdraw-mode.md)

# setWithdrawMode

[Kraken API]\
open fun [setWithdrawMode](set-withdraw-mode.md)(noted: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Sets the withdrawal mode as either a note or item. If the withdrawal mode already matches the provided parameter no action will be taken.

#### Return

True if the withdrawal mode was set correctly and false otherwise.

#### Parameters

Kraken API

| | |
|---|---|
| noted | The boolean representing which withdraw mode to set. When set to false items will be withdrawn while true will withdraw items in a noted format. |
