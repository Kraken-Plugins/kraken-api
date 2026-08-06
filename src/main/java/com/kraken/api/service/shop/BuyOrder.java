package com.kraken.api.service.shop;

/**
 * A purchase from the open shop, bounded by any combination of quantity, price per item and coins
 * spent.
 *
 * <p>A shop charges more for each item as its stock falls, so "buy 500 iron ore" and "buy iron ore
 * while it stays under 30 gp" are different requests, and most plugins want both at once. Set as many
 * limits as apply; the first one to bite stops the order and is reported back as the stop reason.</p>
 *
 * <pre>{@code
 * ShopTransaction bought = shop.buy("Iron ore")
 *         .quantity(500)      // never more than 500
 *         .maxPrice(30)       // stop once the next one costs more than 30 gp
 *         .maxSpend(10_000)   // and never spend more than 10k in total
 *         .execute();
 * }</pre>
 *
 * <p>An order with no limits at all buys until the shop runs dry, the inventory fills or the coins run
 * out, so at least one limit is usually what you want.</p>
 */
public class BuyOrder extends ShopOrder<BuyOrder> {

    BuyOrder(ShopService shop, int itemId, String itemName) {
        super(shop, itemId, itemName);
    }

    @Override
    protected BuyOrder self() {
        return this;
    }

    @Override
    public boolean isSelling() {
        return false;
    }

    /**
     * Stops buying once a single item would cost more than this.
     *
     * <p>Checked before each step, against the price the shop quotes for the next item or the average
     * paid for the previous step. To hold the limit to the exact item, pair it with {@code step(1)} or
     * {@code revalue(true)}.</p>
     *
     * <p>When the shop will not quote a price at all, the order buys a single item to learn what one
     * costs and enforces the limit from there, so exactly one item can slip past it.</p>
     *
     * @param coins the most to pay for one item
     * @return this order
     */
    public BuyOrder maxPrice(int coins) {
        setUnitPriceLimit(coins);
        return this;
    }

    /**
     * Stops buying once this many coins have been spent in total.
     *
     * <p>Spending is measured from the player's coin stack after every step, so the total is exact.
     * The final step can still carry the total slightly past the budget when it buys several items at
     * once; {@code step(1)} removes that overshoot.</p>
     *
     * @param coins the coin budget for the whole order
     * @return this order
     */
    public BuyOrder maxSpend(int coins) {
        setCoinLimit(coins);
        return this;
    }
}
