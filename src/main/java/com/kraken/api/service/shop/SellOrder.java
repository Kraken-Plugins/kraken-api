package com.kraken.api.service.shop;

/**
 * A sale to the open shop, bounded by any combination of quantity, price per item and coins earned.
 *
 * <p>The mirror of {@link BuyOrder}: a shop pays less for each item as its stock of it grows, so a
 * plugin dumping loot usually wants to stop once the price drops past the point where selling is
 * worth the trip. Set as many limits as apply; the first one to bite stops the order.</p>
 *
 * <pre>{@code
 * ShopTransaction sold = shop.sell("Bones")
 *         .quantity(200)          // never more than 200
 *         .minPrice(20)           // stop once the shop pays less than 20 gp each
 *         .targetEarnings(50_000) // and stop once 50k has come in
 *         .execute();
 * }</pre>
 *
 * <p>With no limits set it sells every one the player is carrying.</p>
 */
public class SellOrder extends ShopOrder<SellOrder> {

    SellOrder(ShopService shop, int itemId, String itemName) {
        super(shop, itemId, itemName);
    }

    @Override
    protected SellOrder self() {
        return this;
    }

    @Override
    public boolean isSelling() {
        return true;
    }

    /**
     * Stops selling once the shop would pay less than this for a single item.
     *
     * <p>Checked before each step, against the price the shop quotes for the next item or the average
     * received for the previous step. To hold the limit to the exact item, pair it with {@code step(1)}
     * or {@code revalue(true)}.</p>
     *
     * <p>When the shop will not quote a price at all, the order sells a single item to learn what one
     * fetches and enforces the limit from there, so exactly one item can slip past it.</p>
     *
     * @param coins the least to accept for one item
     * @return this order
     */
    public SellOrder minPrice(int coins) {
        setUnitPriceLimit(coins);
        return this;
    }

    /**
     * Stops selling once this many coins have been taken in.
     *
     * @param coins the earnings target for the whole order
     * @return this order
     */
    public SellOrder targetEarnings(int coins) {
        setCoinLimit(coins);
        return this;
    }
}
