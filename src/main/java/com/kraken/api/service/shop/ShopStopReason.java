package com.kraken.api.service.shop;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Why a {@link BuyOrder} or {@link SellOrder} stopped trading.
 *
 * <p>Every order stops for exactly one reason, and the reason is the point of the result: a plugin
 * that asked for 500 iron ore and got 120 needs to know whether the shop ran dry, the price climbed
 * past the limit, or the coin budget ran out, because each calls for a different next move.</p>
 */
@Getter
@AllArgsConstructor
public enum ShopStopReason {

    /** The full requested quantity was traded. This is the only reason that means "finished". */
    QUANTITY_REACHED("Traded the full requested quantity", true),

    /** The per item price moved past the limit the order set. */
    PRICE_LIMIT_REACHED("The price per item passed the configured limit", true),

    /** The coin budget was spent, or the earnings target was met. */
    COIN_LIMIT_REACHED("The coin limit for this order was reached", true),

    /** The shop has none of the item left to sell. */
    OUT_OF_STOCK("The shop has no more of this item in stock", true),

    /** The player has none of the item left to sell to the shop. */
    NOTHING_LEFT_TO_SELL("The player has no more of this item to sell", true),

    /** The player cannot afford even one more of the item. */
    OUT_OF_COINS("The player cannot afford another one", true),

    /** There is no free inventory slot for an unstackable item. */
    INVENTORY_FULL("The inventory is full", true),

    /** The shop interface is not open, or was closed part way through. */
    SHOP_CLOSED("The shop interface is not open", false),

    /** The item is not on the shop's shelves, or not in the inventory when selling. */
    ITEM_NOT_FOUND("The item could not be found to trade", false),

    /**
     * The shop offers no buy or sell action for the item.
     *
     * <p>How a shop says it does not deal in something: it simply puts no trade actions on it. This
     * is the definitive answer to "will this shop take my loot", and it costs nothing to find out.</p>
     */
    SHOP_WILL_NOT_TRADE("The shop offers no trade action for this item", true),

    /**
     * The shop's smallest trade is larger than the amount still wanted.
     *
     * <p>Reached when a shop offers, say, only "Buy 5" and four items are left to buy. Trading the
     * larger amount instead is not an option, since it would spend the player's coins on stock they
     * did not ask for.</p>
     */
    STEP_TOO_LARGE("The shop's smallest trade is larger than the amount still wanted", true),

    /**
     * A trade was dispatched but nothing moved.
     *
     * <p>The catch all for a shop that will not deal: it refuses the item, the player is not allowed
     * to trade it, or the click did not land. Stopping here rather than looping is deliberate, since
     * a shop that refused once will refuse again.</p>
     */
    NO_PROGRESS("A trade was attempted but nothing changed hands", false),

    /** The order asked for nothing, so nothing was done. */
    NOTHING_REQUESTED("The order requested no items", true);

    /** A short, human readable explanation, suitable for logging or an overlay. */
    private final String description;

    /**
     * Whether the order ran to a clean, expected stopping point.
     *
     * <p>Hitting a price limit is expected; the shop interface closing underneath the order is not.</p>
     */
    private final boolean expected;
}
