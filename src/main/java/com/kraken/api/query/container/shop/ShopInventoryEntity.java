package com.kraken.api.query.container.shop;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractEntity;
import com.kraken.api.query.container.ContainerItem;
import com.kraken.api.service.shop.ShopActions;
import com.kraken.api.service.shop.ShopService;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * One item in the player's inventory while a shop is open, and the thing you sell.
 *
 * <p>The shop draws the inventory in its own panel, so the sell actions live on those widgets rather
 * than on the normal inventory tab. Obtained from {@code ctx.shopInventory()}.</p>
 */
@Slf4j
public class ShopInventoryEntity extends AbstractEntity<ContainerItem> {

    public ShopInventoryEntity(Context ctx, ContainerItem raw) {
        super(ctx, raw);
    }

    @Override
    public int getId() {
        ContainerItem raw = raw();
        return raw != null ? raw.getId() : -1;
    }

    @Override
    public String getName() {
        ContainerItem raw = raw();
        return raw != null ? raw.getName() : null;
    }

    @Override
    public boolean interact(String action) {
        ContainerItem raw = raw();
        if (raw == null) {
            return false;
        }

        ctx.getInteractionManager().interact(raw, action);
        return true;
    }

    /**
     * The size of this stack.
     *
     * @return the quantity held in this slot, or 0 when the item has gone
     */
    public int count() {
        ContainerItem raw = raw();
        return raw != null ? raw.getQuantity() : 0;
    }

    /**
     * Every menu action the shop offers on this item, e.g. "Value", "Sell 1", "Sell 10".
     *
     * @return the non-empty actions, in menu order; never null
     */
    public List<String> actions() {
        ContainerItem raw = raw();
        if (raw == null || raw.getWidget() == null || raw.getWidget().getActions() == null) {
            return Collections.emptyList();
        }

        return Arrays.stream(raw.getWidget().getActions())
                .filter(Objects::nonNull)
                .filter(action -> !action.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * The fixed sell quantities this shop offers on this item, largest first.
     *
     * @return the quantities read off the widget, e.g. {@code [50, 10, 5, 1]}; empty when the shop
     *         will not buy this item
     */
    public int[] sellQuantities() {
        ContainerItem raw = raw();
        if (raw == null || raw.getWidget() == null) {
            return new int[0];
        }
        return ShopActions.fixedQuantities(raw.getWidget().getActions());
    }

    /**
     * Whether this shop will buy this item.
     *
     * <p>A shop only puts sell actions on items it deals in, so an empty action list is the shop
     * saying no before anything is clicked.</p>
     *
     * @return true when the shop offers at least one sell action for it
     */
    public boolean isSellable() {
        return sellQuantities().length > 0;
    }

    /**
     * Asks the shop what it would pay for a single one of these.
     *
     * <p>Clicks "Value" and waits for the shop to answer, so it blocks for up to a couple of game
     * ticks and must not be called from the client thread. The answer is what the shop pays for the
     * <em>next</em> one, which falls as the shop takes more of them.</p>
     *
     * @return the price in coins, or -1 when the shop did not answer in time
     */
    public int value() {
        return ctx.getService(ShopService.class).value(this);
    }

    /**
     * Sells one of this item.
     *
     * @return true when the interaction was dispatched
     */
    public boolean sellOne() {
        return sell(1);
    }

    /**
     * Sells five of this item.
     *
     * @return true when the interaction was dispatched
     */
    public boolean sellFive() {
        return sell(5);
    }

    /**
     * Sells ten of this item.
     *
     * @return true when the interaction was dispatched
     */
    public boolean sellTen() {
        return sell(10);
    }

    /**
     * Sells fifty of this item.
     *
     * @return true when the interaction was dispatched
     */
    public boolean sellFifty() {
        return sell(50);
    }

    /**
     * Sells exactly {@code amount} of this item.
     *
     * <p>Uses the shop's own action when it offers one for that amount, and otherwise breaks the
     * amount into the largest steps the shop does offer, so it never sells more than was asked for.</p>
     *
     * <p>This is the low level, fire and forget form. Use {@code ShopService.sell(...)} when the sale
     * needs to respect a price or earnings limit, or to know what it actually brought in.</p>
     *
     * @param amount how many to sell; values below one do nothing
     * @return true when at least one sell interaction was dispatched
     */
    public boolean sell(int amount) {
        return ctx.getService(ShopService.class).sellNow(this, amount);
    }
}
