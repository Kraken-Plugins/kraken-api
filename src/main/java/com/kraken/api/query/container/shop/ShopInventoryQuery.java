package com.kraken.api.query.container.shop;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractQuery;
import com.kraken.api.query.container.ContainerItem;
import com.kraken.api.service.shop.ShopService;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.widgets.Widget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A fluent query over the player's inventory <em>as the shop sees it</em>.
 *
 * <p>The shop draws the inventory in its own side panel with its own widgets, so selling has to
 * target those and not the normal inventory tab — the same split as {@code bankInventory()} versus
 * {@code inventory()}. Use this to sell; use {@code ctx.inventory()} for everything else.</p>
 *
 * <p>Returns nothing when no shop is open. Usage:</p>
 *
 * <pre>{@code
 * ctx.shopInventory().withName("Bones").first().sell(10);
 * ctx.shopInventory().sellable().list();
 * }</pre>
 */
public class ShopInventoryQuery extends AbstractQuery<ShopInventoryEntity, ShopInventoryQuery, ContainerItem> {

    private static final int PLACEHOLDER_ITEM_ID = 6512;

    public ShopInventoryQuery(Context ctx) {
        super(ctx);
    }

    @Override
    protected Supplier<Stream<ShopInventoryEntity>> source() {
        return () -> {
            List<ShopInventoryEntity> items = ctx.runOnClientThread(() -> {
                Widget panel = ctx.getClient().getWidget(InterfaceID.Shopside.ITEMS);
                if (panel == null || panel.isHidden()) {
                    return Collections.<ShopInventoryEntity>emptyList();
                }

                ItemContainer container = ctx.getClient().getItemContainer(InventoryID.INV);
                if (container == null) {
                    return Collections.<ShopInventoryEntity>emptyList();
                }

                Widget[] slots = panel.getDynamicChildren();
                List<ShopInventoryEntity> found = new ArrayList<>();
                Item[] contents = container.getItems();

                for (int slot = 0; slot < contents.length; slot++) {
                    final Item item = contents[slot];
                    if (item == null || item.getId() == -1 || item.getId() == PLACEHOLDER_ITEM_ID) {
                        continue;
                    }

                    ItemComposition composition = ctx.getClient().getItemDefinition(item.getId());

                    Widget widget = slots != null && slot < slots.length ? slots[slot] : null;
                    found.add(new ShopInventoryEntity(ctx, new ContainerItem(item, composition, slot, ctx,
                            widget, ContainerItem.ItemOrigin.SHOP_INVENTORY)));
                }
                return found;
            });

            return items == null ? Stream.empty() : items.stream();
        };
    }

    /**
     * Filters for items this shop will actually take.
     *
     * <p>A shop only puts "Sell" actions on items it deals in, so this is a reliable answer to "will
     * this shop buy my loot" without having to try it and read the refusal.</p>
     *
     * @return ShopInventoryQuery
     */
    public ShopInventoryQuery sellable() {
        return filter(ShopInventoryEntity::isSellable);
    }

    /**
     * Filters for items in a specific inventory slot.
     *
     * @param slot the inventory slot, 0 to 27
     * @return ShopInventoryQuery
     */
    public ShopInventoryQuery inSlot(int slot) {
        return filter(item -> item.raw().getSlot() == slot);
    }

    /**
     * Filters for stacks of at least {@code quantity}.
     *
     * @param quantity the smallest acceptable stack size, inclusive
     * @return ShopInventoryQuery
     */
    public ShopInventoryQuery withMinimumQuantity(int quantity) {
        return filter(item -> item.count() >= quantity);
    }

    /**
     * How many of an item the player is carrying, counting stack sizes.
     *
     * @param itemId the item id to count
     * @return the total number held, 0 when none
     */
    public int count(int itemId) {
        return withId(itemId).stream().mapToInt(ShopInventoryEntity::count).sum();
    }

    /**
     * Whether the shop interface is open.
     *
     * @return true when a shop is open
     */
    public boolean isOpen() {
        return ctx.getService(ShopService.class).isOpen();
    }
}
