//[kraken-api](../../../index.md)/[com.kraken.api.query.container.bank](../index.md)/[DepositBoxQuery](index.md)/[noted](noted.md)

# noted

[Kraken API]\
open fun [noted](noted.md)(): [DepositBoxQuery](index.md)

Filters the `DepositBoxQuery` to include only items that are noted. 

An item is considered &quot;noted&quot; if it is a placeholder or tradeable voucher referring to a stackable quantity of the item rather than the physical item itself. This method relies on the `isNoted()` method of the underlying item's composition to determine its noted state.

#### Return

DepositBoxQuery A refined query instance including only the items from the deposit box that are in a noted state.
