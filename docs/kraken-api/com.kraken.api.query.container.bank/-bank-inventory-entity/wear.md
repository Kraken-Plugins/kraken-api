//[kraken-api](../../../index.md)/[com.kraken.api.query.container.bank](../index.md)/[BankInventoryEntity](index.md)/[wear](wear.md)

# wear

[Kraken API]\
open fun [wear](wear.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Attempts to wear an item in the inventory while the bank interface is open. 

 The action is typically used to equip wearable items such as armor or accessories from the player's bank inventory. 

 Note: This method does not validate whether the &quot;Wear&quot; action is supported for the current bank inventory entity, nor does it handle cases where this action fails or is unavailable. Validation and exception handling should be implemented as needed in upstream logic.

#### Return

`true` if the &quot;Wear&quot; action is invoked without encountering any internal errors. The return value does not guarantee the success of the action within the context of the game.
