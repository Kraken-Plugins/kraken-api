package com.kraken.api.service.shop;

/**
 * A purchase from the open shop, bounded by any combination of quantity, price per item and coins
 * spent.
 *
 * <p>A shop charges more for each item as its stock falls, so "buy 500 iron ore" and "buy iron ore
 * while it stays under 30 gp" are different requests, and most plugins want both at once. Set as many
 * limits as apply; the first one to bite stops the order and is reported back as the stop reason.</p>
 *
 * <p>Price and coin limits are enforced against the best price the order knows. By default that is
 * the average paid in the previous step, so a step can overshoot by however much the price rose since.
 * They are hard limits only with {@code revalue(true).step(1)}: every item is then quoted before it is
 * bought, and an order that cannot get a quote stops with {@link ShopStopReason#PRICE_UNKNOWN} rather
 * than trading blind. Even then a quote is not a reservation; another player can move the price between
 * the quote and the purchase.</p>
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
     * paid for the previous step. To hold the limit to the exact item, pair it with
     * {@code revalue(true).step(1)}.</p>
     *
     * <p>When the shop will not quote a price at all, the order stops with
     * {@link ShopStopReason#PRICE_UNKNOWN} before buying anything.</p>
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
     * <p>Spending is measured from the player's coin stack after every step, so the total reported is
     * exact. How many items fit in the remaining budget is estimated from the last known price, so the
     * final step can carry the total past the budget by the price rise since that estimate. Only
     * {@code revalue(true).step(1)} holds the budget exactly, at one quote and one click per item.</p>
     *
     * @param coins the coin budget for the whole order
     * @return this order
     */
    public BuyOrder maxSpend(int coins) {
        setCoinLimit(coins);
        return this;
    }
}
