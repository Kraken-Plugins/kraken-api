//[lib](../../../index.md)/[com.kraken.api.query.container.bank](../index.md)/[BankInventoryEntity](index.md)/[wieldOrWear](wield-or-wear.md)

# wieldOrWear

[Kraken API]\
open fun [wieldOrWear](wield-or-wear.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Attempts to wield or wear an item in the inventory while the bank interface is open. The method will find the first matching action on [BankInventoryEntity](index.md) either &quot;wield&quot; or &quot;wear&quot; and invoke the action. This is useful when you have a combination of weapons and items in the inventory that need to be equipped or wielded.

#### Return

True if the wield/wear action was successfully invoked and false otherwise.
