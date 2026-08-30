package com.kraken.api.query.container.inventory;

import com.kraken.api.Context;
import com.kraken.api.query.container.AbstractContainerQuery;
import com.kraken.api.query.container.ContainerItem;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.widgets.Widget;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class InventoryQuery extends AbstractContainerQuery<InventoryEntity, InventoryQuery, ContainerItem> {

    public InventoryQuery(Context ctx) {
        super(ctx);
    }

    @Override
    protected Supplier<Stream<InventoryEntity>> source() {
        return () -> {
            List<InventoryEntity> inventoryEntities = ctx.runOnClientThread(() -> {
                ctx.getClient().runScript(6009, 9764864, 28, 1, -1);

                ItemContainer container = ctx.getClient().getItemContainer(InventoryID.INV);
                if(container == null) return Collections.emptyList();

                Widget inventory = ctx.getClient().getWidget(149, 0);
                if(inventory == null) return Collections.emptyList();

                Widget[] inventoryWidgets = inventory.getDynamicChildren();

                List<InventoryEntity> entities = new ArrayList<>();
                for (int i = 0; i < container.getItems().length; i++) {
                    final Item item = container.getItems()[i];
                    if (item.getId() == -1 || item.getId() == 6512) continue;

                    final ItemComposition itemComposition = ctx.getClient().getItemDefinition(item.getId());
                    if (itemComposition == null) continue;

                    Widget widget = null;
                    if (i < inventoryWidgets.length) {
                        widget = inventoryWidgets[i];
                    }

                    entities.add(new InventoryEntity(ctx, new ContainerItem(item, itemComposition, i, ctx, widget, null)));
                }

                return entities;
            });

            return inventoryEntities.stream();
        };
    }

    /**
     * Sorts the inventory query results based on the specified {@code InventoryOrder}.
     * <p>
     * This method applies the given {@code InventoryOrder}'s comparator to
     * sort inventory items based on the desired order or pattern.
     * </p>
     *
     * @param order The {@code InventoryOrder} specifying the sorting strategy.
     *              <ul>
     *                  <li>{@literal @}TOP_LEFT_BOTTOM_RIGHT - Standard reading order: Row 1 (Left{@literal ->}Right),
     *                      Row 2 (Left{@literal ->}Right), etc.</li>
     *                  <li>{@literal @}BOTTOM_RIGHT_TOP_LEFT - Reverse reading order: Last Item {@literal ->} First Item.</li>
     *                  <li>{@literal @}ZIG_ZAG - Snake/Zig-Zag pattern: Row 1 (Left{@literal ->}Right),
     *                      Row 2 (Right{@literal ->}Left), Row 3 (Left{@literal ->}Right), etc.</li>
     *                  <li>{@literal @}ZIG_ZAG_REVERSE - Reverse Snake/Zig-Zag pattern starting from the bottom right.</li>
     *                  <li>{@literal @}TOP_DOWN_LEFT_RIGHT - Vertical columns:
     *                      Column 1 (Top{@literal ->}Bottom), Column 2 (Top{@literal ->}Bottom), etc.</li>
     *              </ul>
     *
     * @return An {@code InventoryQuery} object containing the inventory items
     *         sorted based on the given order.
     */
    public InventoryQuery orderBy(InventoryOrder order) {
         return sorted(order.getComparator());
    }

    /**
     * Returns true if the inventory is full and false otherwise.
     * @return True if the inventory is full and false otherwise.
     */
    public boolean isFull() {
        return source().get().count() >= 28;
    }

    /**
     * Returns the free space in a users inventory.
     * @return The amount of free space available in the players inventory
     */
    public int freeSpace() {
        return Math.toIntExact(28 - source().get().count());
    }

    /**
     * Returns a list of Inventory Items which can be consumed for health.
     * @return List of Inventory items which are food.
     */
    public InventoryQuery food() {
        return filter(i -> i.raw().isFood());
    }

    /**
     * Returns true when the player has edible hard food in their inventory and false otherwise.
     * @return boolean
     */
    public boolean hasFood() {
        return filter(i -> i.raw().isFood()).count() > 0;
    }
}