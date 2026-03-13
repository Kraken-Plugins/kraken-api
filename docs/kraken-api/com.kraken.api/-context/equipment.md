//[kraken-api](../../../index.md)/[com.kraken.api](../index.md)/[Context](index.md)/[equipment](equipment.md)

# equipment

[Kraken API]\
open fun [equipment](equipment.md)(): [EquipmentQuery](../../com.kraken.api.query.equipment/-equipment-query/index.md)

Creates a new query builder for the equipment interface. Usage: ctx.equipment().inSlot(EquipmentInventorySlot.HEAD).interact(&quot;Remove&quot;); ctx.equipment().withId(1234).interact(&quot;Wield&quot;);

#### Return

EquipmentQuery object used to chain together predicates to select specific items or groups of items within the players equipment or inventory interface. Only items with the action &quot;wield&quot; or &quot;wear&quot; will be interactable using this query from the inventory.
