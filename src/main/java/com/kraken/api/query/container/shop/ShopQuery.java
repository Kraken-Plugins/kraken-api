package com.kraken.api.query.container.shop;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractQuery;
import com.kraken.api.service.shop.ShopService;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A fluent query over the items on the open shop's shelves.
 *
 * <p>Shop stock is not held in an item container the client can read — every shop in the game has its
 * own container id — so this reads the interface itself, which is also what makes the results
 * directly interactable.</p>
 *
 * <p>Returns nothing at all when no shop is open. Usage:</p>
 *
 * <pre>{@code
 * ctx.shop().withName("Iron ore").first().buy(50);
 * ctx.shop().inStock().withMinimumStock(100).list();
 * ctx.shop().nameContains("rune").sortedByStock().first();
 * }</pre>
 *
 * @see ShopService for opening a shop and for purchases that respect price and coin limits
 */
@Slf4j
public class ShopQuery extends AbstractQuery<ShopEntity, ShopQuery, Widget> {

    public ShopQuery(Context ctx) {
        super(ctx);
    }

    @Override
    protected Supplier<Stream<ShopEntity>> source() {
        return () -> {
            List<ShopEntity> items = ctx.runOnClientThread(() -> {
                Widget container = ctx.getClient().getWidget(InterfaceID.Shopmain.ITEMS);
                if (container == null || container.isHidden()) {
                    return Collections.<ShopEntity>emptyList();
                }

                Widget[] slots = container.getDynamicChildren();
                if (slots == null) {
                    return Collections.<ShopEntity>emptyList();
                }

                List<ShopEntity> found = new ArrayList<>();
                for (Widget slot : slots) {
                    // Shops pad their shelves with empty slots; only the ones holding an item are real.
                    if (slot == null || slot.getItemId() <= 0) {
                        continue;
                    }
                    found.add(new ShopEntity(ctx, slot));
                }
                return found;
            });

            return items == null ? Stream.empty() : items.stream();
        };
    }

    /**
     * Filters for items the shop can actually sell right now.
     *
     * <p>Sold out items stay on the shelves at a stock of zero, so this is the filter that separates
     * "the shop deals in this" from "the shop has this".</p>
     *
     * @return ShopQuery
     */
    public ShopQuery inStock() {
        return filter(ShopEntity::isInStock);
    }

    /**
     * Filters for items the shop has run out of.
     *
     * @return ShopQuery
     */
    public ShopQuery outOfStock() {
        return filter(item -> !item.isInStock());
    }

    /**
     * Filters for items the shop holds at least {@code quantity} of.
     *
     * @param quantity the smallest acceptable stock level, inclusive
     * @return ShopQuery
     */
    public ShopQuery withMinimumStock(int quantity) {
        return filter(item -> item.stock() >= quantity);
    }

    /**
     * Filters for items in a specific slot on the shelves.
     *
     * @param slot the slot index, counting from zero
     * @return ShopQuery
     */
    public ShopQuery inSlot(int slot) {
        return filter(item -> item.slot() == slot);
    }

    /**
     * Filters for items offering a specific menu action, e.g. "Buy 50".
     *
     * @param action the action to look for; case-insensitive
     * @return ShopQuery
     */
    public ShopQuery withAction(String action) {
        return filter(item -> item.actions().stream().anyMatch(a -> a.equalsIgnoreCase(action)));
    }

    /**
     * Sorts the results by stock level, deepest stock first.
     *
     * <p>The cheapest items are the ones the shop is overstocked on, so this doubles as a rough
     * cheapest-first ordering.</p>
     *
     * @return ShopQuery
     */
    public ShopQuery sortedByStock() {
        return sorted((first, second) -> Integer.compare(second.stock(), first.stock()));
    }

    /**
     * Whether the shop stocks an item at all, sold out or not.
     *
     * @param itemId the item id to look for
     * @return true when the shop has a slot for it
     */
    public boolean stocks(int itemId) {
        return withId(itemId).isPresent();
    }

    /**
     * Whether the shop stocks an item at all, sold out or not.
     *
     * @param name the item name to look for; case-insensitive and must be the full name
     * @return true when the shop has a slot for it
     */
    public boolean stocks(String name) {
        return withName(name).isPresent();
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
