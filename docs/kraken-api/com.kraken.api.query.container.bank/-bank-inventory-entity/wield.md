//[kraken-api](../../../index.md)/[com.kraken.api.query.container.bank](../index.md)/[BankInventoryEntity](index.md)/[wield](wield.md)

# wield

[Kraken API]\
open fun [wield](wield.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Attempts to wield an item in the inventory while the bank interface is open `BankInventoryEntity`. 

 This action is typically used to equip items such as weapons or tools from the player's bank inventory. 

Note: This method does not validate if the &quot;Wield&quot; action is supported for the current bank inventory entity or if the action is successful within the game. Additional validation and error handling should be implemented in the calling logic as necessary. 

#### Return

`true` if the &quot;Wield&quot; interaction is invoked successfully without any internal client error. The return value does not guarantee that the action completes as intended within the game.
