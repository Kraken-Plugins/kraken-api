//[lib](../../../index.md)/[com.kraken.api.service.ui.processing](../index.md)/[ProcessingService](index.md)/[setAmount](set-amount.md)

# setAmount

[Kraken API]\
open fun [setAmount](set-amount.md)(amount: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html))

Sets the skill multi-quantity value to the specified amount. 

This method first compares the current value of the skill multi-quantity variable with the provided `amount`. If they are the same, the method exits early without making any changes.

If the values differ, the method updates the `VarClientID.SKILLMULTI_QUANTITY` variable to the specified `amount` by executing the update logic on the game client's thread. This ensures thread safety and avoids potential concurrency issues.

#### Parameters

Kraken API

| | |
|---|---|
| amount | The integer value to set as the new skill multi-quantity. It represents the selected quantity in the &quot;Make-X&quot; interface or similar functionality. |
