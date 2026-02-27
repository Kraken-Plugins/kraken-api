//[lib](../../../index.md)/[com.kraken.api.query.container.inventory](../index.md)/[InventoryQuery](index.md)/[orderBy](order-by.md)

# orderBy

[Kraken API]\
open fun [orderBy](order-by.md)(order: [InventoryOrder](../-inventory-order/index.md)): [InventoryQuery](index.md)

Sorts the inventory query results based on the specified `InventoryOrder`. 

 This method applies the given `InventoryOrder`'s comparator to sort inventory items based on the desired order or pattern. 

#### Return

An `InventoryQuery` object containing the inventory items sorted based on the given order.

#### Parameters

Kraken API

| | |
|---|---|
| order | The `InventoryOrder` specifying the sorting strategy. <br>- @TOP_LEFT_BOTTOM_RIGHT - Standard reading order: Row 1 (Left-&gt;Right), Row 2 (Left-&gt;Right), etc. - @BOTTOM_RIGHT_TOP_LEFT - Reverse reading order: Last Item -&gt; First Item. - @ZIG_ZAG - Snake/Zig-Zag pattern: Row 1 (Left-&gt;Right), Row 2 (Right-&gt;Left), Row 3 (Left-&gt;Right), etc. - @ZIG_ZAG_REVERSE - Reverse Snake/Zig-Zag pattern starting from the bottom right. - @TOP_DOWN_LEFT_RIGHT - Vertical columns: Column 1 (Top-&gt;Bottom), Column 2 (Top-&gt;Bottom), etc. |
