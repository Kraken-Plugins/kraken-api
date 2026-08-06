package com.kraken.api.query.container.shop;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractEntity;
import com.kraken.api.service.shop.ShopActions;
import com.kraken.api.service.shop.ShopService;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ItemComposition;
import net.runelite.api.widgets.Widget;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * One item on a shop's shelves.
 *
 * <p>Wraps the real interface widget for the slot, so the buy actions dispatched here are the same
 * ones the client offers on a right click, and the quantities available are read back from the widget
 * rather than assumed.</p>
 *
 * <p>Obtained from {@code ctx.shop()}, which only returns anything while a shop is open:</p>
 *
 * <pre>{@code
 * ctx.shop().withName("Iron ore").first().buy(50);
 * ctx.shop().inStock().nameContains("rune").list();
 * }</pre>
 */
@Slf4j
public class ShopEntity extends AbstractEntity<Widget> {

    public ShopEntity(Context ctx, Widget raw) {
        super(ctx, raw);
    }

    @Override
    public int getId() {
        Widget raw = raw();
        return raw != null ? raw.getItemId() : -1;
    }

    @Override
    public String getName() {
        Widget raw = raw();
        if (raw == null) {
            return null;
        }

        return ctx.runOnClientThreadOptional(() -> {
            ItemComposition composition = ctx.getItemManager().getItemComposition(raw.getItemId());
            return composition.getName();
        }).orElse(null);
    }

    @Override
    public boolean interact(String action) {
        Widget raw = raw();
        if (raw == null) {
            return false;
        }

        return ctx.getInteractionManager().interact(raw, action);
    }

    /**
     * How many of this item the shop currently has.
     *
     * <p>Shops keep sold out items on the shelf at a stock of zero so they can restock, so a slot
     * existing does not mean anything is buyable.</p>
     *
     * @return the number in stock, or 0 when sold out
     */
    public int stock() {
        Widget raw = raw();
        return raw != null ? raw.getItemQuantity() : 0;
    }

    /**
     * Whether the shop has at least one of this item to sell.
     *
     * @return true when {@link #stock()} is above zero
     */
    public boolean isInStock() {
        return stock() > 0;
    }

    /**
     * The slot this item occupies on the shelves, counting from zero.
     *
     * @return the slot index, or -1 when the widget has gone
     */
    public int slot() {
        Widget raw = raw();
        return raw != null ? raw.getIndex() : -1;
    }

    /**
     * Every menu action the shop offers on this item, e.g. "Value", "Buy 1", "Buy 10".
     *
     * @return the non-empty actions, in menu order; never null
     */
    public List<String> actions() {
        Widget raw = raw();
        if (raw == null || raw.getActions() == null) {
            return Collections.emptyList();
        }

        return Arrays.stream(raw.getActions())
                .filter(Objects::nonNull)
                .filter(action -> !action.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * The fixed buy quantities this shop offers on this item, largest first.
     *
     * @return the quantities read off the widget, e.g. {@code [50, 10, 5, 1]}
     */
    public int[] buyQuantities() {
        Widget raw = raw();
        return raw == null ? new int[0] : ShopActions.fixedQuantities(raw.getActions());
    }

    /**
     * Asks the shop what a single one of these costs.
     *
     * <p>Clicks "Value" and waits for the shop to answer, so it blocks for up to a couple of game
     * ticks and must not be called from the client thread. The answer is the price of the
     * <em>next</em> one, which climbs as the shop's stock falls.</p>
     *
     * @return the price in coins, or -1 when the shop did not answer in time
     */
    public int value() {
        return ctx.getService(ShopService.class).value(this);
    }

    /**
     * Buys one of this item.
     *
     * @return true when the interaction was dispatched
     */
    public boolean buyOne() {
        return buy(1);
    }

    /**
     * Buys five of this item.
     *
     * @return true when the interaction was dispatched
     */
    public boolean buyFive() {
        return buy(5);
    }

    /**
     * Buys ten of this item.
     *
     * @return true when the interaction was dispatched
     */
    public boolean buyTen() {
        return buy(10);
    }

    /**
     * Buys fifty of this item.
     *
     * @return true when the interaction was dispatched
     */
    public boolean buyFifty() {
        return buy(50);
    }

    /**
     * Buys exactly {@code amount} of this item.
     *
     * <p>Uses the shop's own action when it offers one for that amount, and otherwise breaks the
     * amount into the largest steps the shop does offer — 37 becomes 10, 10, 10, 5, 1, 1. That never
     * overshoots, so it cannot spend coins on stock that was not asked for.</p>
     *
     * <p>This is the low level, fire and forget form: it dispatches clicks and returns. Use
     * {@code ShopService.buy(...)} when the purchase needs to respect a price or coin limit, or to
     * know what it actually cost.</p>
     *
     * @param amount how many to buy; values below one do nothing
     * @return true when at least one buy interaction was dispatched
     */
    public boolean buy(int amount) {
        return ctx.getService(ShopService.class).buyNow(this, amount);
    }
}
