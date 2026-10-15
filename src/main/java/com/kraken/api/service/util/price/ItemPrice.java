package com.kraken.api.service.util.price;


import lombok.Builder;
import lombok.Value;

/**
 * An immutable snapshot of an item's Grand Exchange prices as reported by RuneLite's
 * {@link net.runelite.client.game.ItemManager}.
 *
 * <h2>Attributes:</h2>
 * <ul>
 *   <li><strong>itemId:</strong> The unique identifier for the item.</li>
 *   <li><strong>wikiPrice:</strong> The OSRS Wiki real-time price RuneLite distributes with its price list.
 *   RuneLite substitutes the Grand Exchange guide price when the wiki price is missing, below its
 *   low-price threshold, or too far from the guide price to be trusted. Prefer this for trading decisions.</li>
 *   <li><strong>gePrice:</strong> The official Grand Exchange guide price, which updates far less often than the
 *   wiki price.</li>
 * </ul>
 *
 * <p>Either price may be {@code 0} when that source has no data for the item, but never both.</p>
 */
@Value
@Builder
public class ItemPrice {
    int itemId;
    long wikiPrice;
    long gePrice;
}
