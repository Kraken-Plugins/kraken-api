package com.kraken.api.service.shop;

import lombok.Getter;
import lombok.Setter;

/**
 * The settings shared by a {@link BuyOrder} and a {@link SellOrder}.
 *
 * <p>An order is a description of when to stop, not a script. It is handed to {@link ShopService},
 * which trades in steps and re-checks every limit between them, because a shop's price moves with its
 * stock and neither side of the trade can be planned up front.</p>
 *
 * @param <T> the concrete order type, so the fluent setters keep returning it
 */
@Setter
@Getter
public abstract class ShopOrder<T extends ShopOrder<T>> {

    /** The value a limit holds when it is not set. */
    public static final int NO_LIMIT = -1;

    private final ShopService shop;

    /** The item to trade, or {@link #NO_LIMIT} when the order names it instead. */
    private final int itemId;

    /** The item to trade, or null when the order identifies it by id. */
    private final String itemName;

    /** The most items to trade. Unlimited by default, so a limit must be what stops the order. */
    private int quantity = Integer.MAX_VALUE;

    /** The per item price limit, or {@link #NO_LIMIT}. Read as a maximum or minimum by direction. */
    private int unitPriceLimit = NO_LIMIT;

    /** The coin limit, or {@link #NO_LIMIT}. Read as a budget or a target by direction. */
    private int coinLimit = NO_LIMIT;

    /** The largest number of items to trade in one click. */
    private int step = ShopQuantity.FIFTY.getAmount();

    /** Whether to ask the shop for a fresh quote before every step. */
    private boolean revalue = false;

    protected ShopOrder(ShopService shop, int itemId, String itemName) {
        this.shop = shop;
        this.itemId = itemId;
        this.itemName = itemName;
    }

    /**
     * Returns this order as its concrete type, so the shared setters stay chainable.
     *
     * @return this
     */
    protected abstract T self();

    /**
     * Which side of the counter this order is on.
     *
     * @return true when selling to the shop, false when buying from it
     */
    public abstract boolean isSelling();

    /**
     * Caps how many items the order trades.
     *
     * @param quantity the most items to trade; values below one make the order a no-op
     * @return this order
     */
    public T quantity(int quantity) {
        this.quantity = quantity;
        return self();
    }

    /**
     * Caps how many items are traded per click.
     *
     * <p>Limits are only re-checked between steps, so the step size is also the granularity at which
     * they are enforced. The default of 50 is the largest single trade a shop offers and is right for
     * bulk work. Dropping to 1 enforces a price limit to the exact item, at the cost of one click per
     * item.</p>
     *
     * @param step the largest number of items to trade at once; clamped to at least 1
     * @return this order
     */
    public T step(int step) {
        this.step = Math.max(1, step);
        return self();
    }

    /**
     * Asks the shop to re-quote the price before every step.
     *
     * <p>Off by default, because the price paid per item is already measured exactly from the player's
     * coin stack after each step, which costs nothing extra. Turn it on when the order must react to
     * the price of the <em>next</em> item rather than the average of the last step — for instance when
     * buying in steps of 50 with a tight per item limit.</p>
     *
     * @param revalue true to value the item before each step
     * @return this order
     */
    public T revalue(boolean revalue) {
        this.revalue = revalue;
        return self();
    }

    /**
     * Runs the order against the open shop.
     *
     * <p>Blocks while it trades, so it must be called off the client thread — from a {@code Script}
     * loop or a worker, never from a client callback.</p>
     *
     * @return what was traded and why it stopped
     */
    public ShopTransaction execute() {
        return shop.execute(this);
    }

    /**
     * Whether a per item price limit was set.
     *
     * @return true when {@code getUnitPriceLimit()} holds a real limit rather than {@link #NO_LIMIT}
     */
    public boolean hasPriceLimit() {
        return unitPriceLimit != NO_LIMIT;
    }

    /**
     * Whether a coin budget or earnings target was set.
     *
     * @return true when {@code getCoinLimit()} holds a real limit rather than {@link #NO_LIMIT}
     */
    public boolean hasCoinLimit() {
        return coinLimit != NO_LIMIT;
    }
}
