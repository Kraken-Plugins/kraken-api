package com.kraken.api.service.shop;

import lombok.Builder;
import lombok.Value;

/**
 * What a completed {@link BuyOrder} or {@link SellOrder} actually did.
 *
 * <p>The coin figures come from watching the player's coin stack rather than from multiplying a
 * quoted price by a quantity, so they are exact even though a shop's price climbs with every item
 * bought. A shop that trades in something other than coins reports zero here while still reporting
 * the quantity it moved.</p>
 */
@Value
@Builder
public class ShopTransaction {

    /** True when this was a sale to the shop, false when it was a purchase. */
    boolean selling;

    /** The item that was traded. */
    int itemId;

    /** The item's name, for logging. */
    String itemName;

    /** How many were traded. Zero when the order stopped before moving anything. */
    int quantity;

    /**
     * Coins spent when buying, or received when selling.
     *
     * <p>Measured from the player's coin stack, so it accounts for the price drift that happens as
     * stock moves. Zero for a shop that trades in another currency.</p>
     */
    int coins;

    /** Why the order stopped. */
    ShopStopReason stopReason;

    /**
     * The average coins per item over the whole order.
     *
     * <p>Not the price of the next one: a shop's price moves as its stock does, so this sits between
     * the first and last unit price.</p>
     *
     * @return the mean unit price, or 0 when nothing was traded
     */
    public double getAveragePrice() {
        return quantity == 0 ? 0 : (double) coins / quantity;
    }

    /**
     * Whether the full requested quantity was traded.
     *
     * @return true only when the order stopped because it had traded everything it was asked to
     */
    public boolean isComplete() {
        return stopReason == ShopStopReason.QUANTITY_REACHED;
    }

    /**
     * Whether anything at all was traded.
     *
     * @return true when at least one item changed hands
     */
    public boolean isTraded() {
        return quantity > 0;
    }

    @Override
    public String toString() {
        return String.format("%s %d x %s for %d coins (avg %.1f) - %s",
                selling ? "Sold" : "Bought", quantity, itemName, coins, getAveragePrice(),
                stopReason.getDescription());
    }
}
