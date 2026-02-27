//[lib](../../../index.md)/[com.kraken.api](../index.md)/[Context](index.md)/[inventory](inventory.md)

# inventory

[Kraken API]\
open fun [inventory](inventory.md)(): [InventoryQuery](../../com.kraken.api.query.container.inventory/-inventory-query/index.md)

Creates a new query builder for the standard Backpack Inventory. This is only for finding items in a players inventory and should not be used when the Bank is open to deposit items. Instead, use `BankInventoryQuery` for depositing items. Usage: ctx.inventory().withId(1234).count();

#### Return

InventoryQuery object used to chain together predicates to select specific items or groups of items within the players inventory.
