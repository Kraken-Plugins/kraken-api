package com.kraken.api.service.util.price;

import com.kraken.api.Context;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.game.ItemManager;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Looks up Grand Exchange prices from RuneLite's {@link ItemManager}.
 * <p>
 * RuneLite downloads the full price list once and refreshes it on its own schedule, so lookups are
 * in-memory reads that never make a network request of their own.
 * </p>
 */
@Slf4j
@Singleton
public class ItemPriceService {

    @Inject
    private Context ctx;

    /**
     * Retrieves the price for a specific item.
     * <p>
     * Safe to call from any thread. On the client thread the lookup runs inline; from any other thread
     * (a {@link com.kraken.api.core.script.Script} loop, for example) it blocks until the client thread
     * performs it. Noted items, coins, platinum tokens and items RuneLite maps to a tradeable
     * equivalent are priced the same way RuneLite prices them.
     * </p>
     *
     * @param itemId The OSRS item ID
     * @return The item's price, or {@code null} when the item has no price (it is untradeable, RuneLite
     * has not finished loading its price list, or the client thread could not perform the lookup)
     */
    public ItemPrice getItemPrice(int itemId) {
        return ctx.runOnClientThreadOptional(() -> lookupPrice(itemId)).orElse(null);
    }

    /**
     * Reads both of RuneLite's price sources for an item. Must run on the client thread because
     * {@link ItemManager} resolves the item's composition to handle noted and mapped items.
     *
     * @param itemId The OSRS item ID
     * @return The item's price, or {@code null} when neither source has a price for it
     */
    private ItemPrice lookupPrice(int itemId) {
        ItemManager itemManager = ctx.getItemManager();
        long wikiPrice = itemManager.getItemPriceWithSource(itemId, true);
        long gePrice = itemManager.getItemPriceWithSource(itemId, false);

        if (wikiPrice <= 0 && gePrice <= 0) {
            log.debug("No price available for item {}", itemId);
            return null;
        }

        return ItemPrice.builder()
                .itemId(itemId)
                .wikiPrice(wikiPrice)
                .gePrice(gePrice)
                .build();
    }
}
