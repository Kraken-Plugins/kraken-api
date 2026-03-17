//[kraken-api](../../../index.md)/[com.kraken.api.service.bank](../index.md)/[DepositBoxService](index.md)/[close](close.md)

# close

[Kraken API]\
open fun [close](close.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Closes the bank deposit box interface if it is currently open. 

This method checks if the deposit box interface is already closed by invoking [isClosed](is-closed.md). If the interface is open, it attempts to close it by executing a client script.

The operation will return `true` if the interface is successfully verified as closed or was already closed before the method was invoked.

#### Return

`true` if the deposit box interface is closed (either already closed or successfully closed); `false` if the operation cannot determine the state of the interface or failed to close it.
