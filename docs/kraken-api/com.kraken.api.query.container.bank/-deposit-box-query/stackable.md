//[kraken-api](../../../index.md)/[com.kraken.api.query.container.bank](../index.md)/[DepositBoxQuery](index.md)/[stackable](stackable.md)

# stackable

[Kraken API]\
open fun [stackable](stackable.md)(): [DepositBoxQuery](index.md)

Filters the `DepositBoxQuery` to include only items that are stackable. 

This method refines the `DepositBoxQuery` by applying a filtering condition that selects items from the deposit box which are deemed stackable. Stackable items allow multiple units to occupy a single inventory slot. 

- An item is considered stackable if its `isStackable()` method returns `true`.
- If no stackable items are found, the result of this query will be empty.

#### Return

DepositBoxQuery A refined `DepositBoxQuery` instance that includes only the stackable items in the deposit box.
