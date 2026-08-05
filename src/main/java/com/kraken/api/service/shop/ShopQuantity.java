package com.kraken.api.service.shop;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.gameval.InterfaceID;

/**
 * The quantity selector buttons along the bottom of the shop interface.
 *
 * <p>The selected button decides what a plain left click on a shop item buys, and what a plain left
 * click on an inventory item sells. Every fixed quantity is also available as an explicit right click
 * action on the item itself ("Buy 10", "Sell 5"), which is what {@code ShopService} normally uses,
 * so switching the button is only needed when a plugin wants the interface itself left in a
 * particular state.</p>
 */
@Getter
@AllArgsConstructor
public enum ShopQuantity {

    ONE(1, InterfaceID.Shopmain.QUANTITY1),
    FIVE(5, InterfaceID.Shopmain.QUANTITY5),
    TEN(10, InterfaceID.Shopmain.QUANTITY10),
    FIFTY(50, InterfaceID.Shopmain.QUANTITY50),

    /** The player defined amount, entered through a number dialogue. */
    X(-1, InterfaceID.Shopmain.QUANTITYX);

    /** The number of items this button trades, or -1 for {@link #X}. */
    private final int amount;

    /** The packed component id of the button in the shop interface. */
    private final int widgetId;

    /**
     * Whether this button trades a fixed, known number of items.
     *
     * @return true for {@link #ONE}, {@link #FIVE}, {@link #TEN} and {@link #FIFTY}, false for {@link #X}
     */
    public boolean isFixed() {
        return amount > 0;
    }

    /**
     * The largest fixed button that does not trade more than {@code amount} items.
     *
     * <p>Used to turn an arbitrary request into a sequence of button sized trades: asking for 37
     * resolves to {@link #TEN}, leaving 27 for the next round.</p>
     *
     * @param amount the number of items still wanted; values below one have no matching button
     * @return the largest fitting button, or null when {@code amount} is less than one
     */
    public static ShopQuantity fromAmount(int amount) {
        ShopQuantity best = null;
        for (ShopQuantity quantity : values()) {
            if (quantity.isFixed() && quantity.amount <= amount
                    && (best == null || quantity.amount > best.amount)) {
                best = quantity;
            }
        }
        return best;
    }
}
