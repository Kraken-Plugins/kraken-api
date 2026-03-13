//[kraken-api](../../../index.md)/[com.kraken.api.service.ui.processing](../index.md)/[ProcessingService](index.md)/[getAmount](get-amount.md)

# getAmount

[Kraken API]\
open fun [getAmount](get-amount.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)

Retrieves the current value of the skill multi-quantity variable. 

This method executes on the game client's thread. It reads the value associated with `VarClientID.SKILLMULTI_QUANTITY`, which represents the current quantity in the make-X interface or other similar functionality.

The value retrieval ensures thread-safety by invoking the method on the client's thread.

#### Return

The integer value of the @VarClientID.SKILLMULTI_QUANTITY variable, or `0` if no value is set or the retrieval fails.
