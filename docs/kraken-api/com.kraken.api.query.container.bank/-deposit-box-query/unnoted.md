//[kraken-api](../../../index.md)/[com.kraken.api.query.container.bank](../index.md)/[DepositBoxQuery](index.md)/[unnoted](unnoted.md)

# unnoted

[Kraken API]\
open fun [unnoted](unnoted.md)(): [DepositBoxQuery](index.md)

Filters the `DepositBoxQuery` to include only items that are unnoted. 

An item is considered &quot;unnoted&quot; if it is the actual physical item rather than a placeholder or tradeable voucher for a stackable quantity. This method relies on the `isNoted()` method of the raw item to determine its state, and excludes items that are noted.

#### Return

DepositBoxQuery A refined query instance including only the items from the deposit box that are in an unnoted state.
