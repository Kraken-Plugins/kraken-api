//[kraken-api](../../../index.md)/[com.kraken.api.service.bank](../index.md)/[DepositBoxService](index.md)/[depositLootingBag](deposit-looting-bag.md)

# depositLootingBag

[Kraken API]\
open fun [depositLootingBag](deposit-looting-bag.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Deposits all items from the looting bag into the bank deposit box. 

This method interacts with the specific widget representing the &quot;Deposit Looting Bag&quot; option in the bank deposit box interface. It uses the @ctxProvider to access the widget and perform the interaction.

- The operation assumes that the bank deposit box interface is already open and accessible.
- If the interface is not open, the interaction may fail.

#### Return

`true` if the interaction with the &quot;Deposit Looting Bag&quot; widget is successful; `false` otherwise if the widget is not found or the interaction fails.
