//[lib](../../../index.md)/[com.kraken.api](../index.md)/[Context](index.md)/[bankInventory](bank-inventory.md)

# bankInventory

[Kraken API]\
open fun [bankInventory](bank-inventory.md)(): [BankInventoryQuery](../../com.kraken.api.query.container.bank/-bank-inventory-query/index.md)

Creates a new query builder for a Bank Inventory. This should only be used when the bank is open in order to deposit items from the players inventory into the bank. A different parent widget is used for the players inventory while the bank is open compared to the normal players inventory. For querying the players inventory to eat food, interact with objects, or perform general actions without a bank use: `InventoryQuery`. Usage: ctx.bankInventory().withId(1234).count();

#### Return

BankInventoryQuery object used to chain together predicates to select specific items or groups of items within the players inventory while the bank interface is open.
