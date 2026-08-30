package com.kraken.api.service.shop;

import com.google.inject.Provider;
import com.kraken.api.Context;
import com.kraken.api.core.interaction.InteractionManager;
import com.kraken.api.input.KeyboardService;
import com.kraken.api.query.container.shop.ShopEntity;
import com.kraken.api.query.container.shop.ShopInventoryEntity;
import com.kraken.api.query.npc.NpcEntity;
import com.kraken.api.query.npc.NpcQuery;
import com.kraken.api.query.widget.WidgetEntity;
import com.kraken.api.service.ui.UIService;
import com.kraken.api.service.util.SleepService;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.eventbus.Subscribe;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Optional;
import java.awt.event.KeyEvent;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntSupplier;
import java.util.function.Predicate;

/**
 * A service class for trading with NPC shops.
 *
 * <p>Covers the whole round trip: finding a shopkeeper, opening their shop, reading what is on the
 * shelves, and buying or selling under limits. For filtering the shelves themselves use
 * {@code ctx.shop()}, and for the inventory the shop displays alongside them use
 * {@code ctx.shopInventory()}.</p>
 *
 * <pre>{@code
 * ShopService shop = ctx.getService(ShopService.class);
 *
 * shop.open("Shop keeper");
 *
 * // Buy up to 500 iron ore, but stop if the price passes 30 gp or 10k has been spent.
 * ShopTransaction bought = shop.buy("Iron ore")
 *         .quantity(500)
 *         .maxPrice(30)
 *         .maxSpend(10_000)
 *         .execute();
 *
 * log.info("{} for {} coins, stopped because {}",
 *         bought.getQuantity(), bought.getCoins(), bought.getStopReason().getDescription());
 *
 * // Dump loot until the shop stops paying properly for it.
 * shop.sell("Bones").minPrice(20).execute();
 * shop.close();
 * }</pre>
 *
 * <h2>Prices</h2>
 *
 * <p>A shop's prices are the server's, not the client's: nothing in the client holds them, and they
 * move with every item traded. There are therefore two ways this class learns a price, and it uses
 * both. {@link #value(ShopEntity)} clicks "Value" and parses the shop's reply, which gives the price
 * of the <em>next</em> item but costs a round trip. During an order it also measures the player's coin
 * stack across each step, which is exact and free but describes the step just completed. Orders run on
 * the measured price and only pay for a fresh quote when asked to
 * {@link ShopOrder#revalue(boolean) revalue}.</p>
 *
 * <h2>Threading</h2>
 *
 * <p>Everything that waits for the game — opening, closing, valuing, and any order — blocks, so call
 * it off the client thread, from a {@code Script} loop or a worker. The query accessors and the plain
 * state checks are safe anywhere.</p>
 */
@Slf4j
@Singleton
public class ShopService {

    /** The menu action shopkeepers carry, and the one that opens a shop. */
    public static final String TRADE_ACTION = "Trade";

    private static final int OPEN_TIMEOUT_MS = 5000;
    private static final int VALUE_TIMEOUT_MS = 3000;
    private static final int TRADE_TIMEOUT_MS = 3000;

    @Inject
    private Provider<Context> ctxProvider;

    @Inject
    private InteractionManager interactionManager;

    @Inject
    private KeyboardService keyboard;

    /**
     * The most recent price the shop quoted.
     *
     * <p>Held rather than accumulated in a cache because a shop price is only true for the instant it
     * was quoted — caching one would be caching something already stale. Written from the client
     * thread by the event bus and read by whichever thread is waiting on a valuation.</p>
     */
    private final AtomicReference<ShopPrice> lastQuote = new AtomicReference<>();

    /**
     * Records shop price quotes as they arrive.
     *
     * <p>Registered on the event bus by {@link Context}. Every message is offered to the parser, which
     * ignores everything that is not a quote, so unrelated chat costs one failed regex match.</p>
     *
     * @param event the chat message event
     */
    @Subscribe
    public void onChatMessage(ChatMessage event) {
        ShopPrice.parse(event.getMessage()).ifPresent(lastQuote::set);
    }

    /**
     * Whether a shop interface is open.
     *
     * @return true when the shop is open
     */
    public boolean isOpen() {
        Context ctx = ctxProvider.get();
        return ctx.runOnClientThread(() -> {
            Widget shop = ctx.getClient().getWidget(InterfaceID.Shopmain.UNIVERSE);
            return shop != null && !shop.isSelfHidden() && !shop.isHidden();
        });
    }

    /**
     * Whether no shop interface is open.
     *
     * @return true when the shop is closed
     */
    public boolean isClosed() {
        return !isOpen();
    }

    /**
     * Every shopkeeper in the scene, nearest first.
     *
     * <p>A shopkeeper is any NPC offering the "Trade" action, which is how the game itself marks them,
     * so this finds the general store owner, the sawmill operator and the Slayer master's shop without
     * needing to know any of their names. Filter it further like any other NPC query.</p>
     *
     * <pre>{@code
     * ctx.getService(ShopService.class).shopkeepers().within(10).nameContains("Zaff").first();
     * }</pre>
     *
     * @return an NpcQuery over the tradeable NPCs, sorted by distance from the player
     */
    public NpcQuery shopkeepers() {
        return ctxProvider.get().npcs().withAction(TRADE_ACTION).sortByDistance();
    }

    /**
     * The closest shopkeeper.
     *
     * @return the nearest NPC offering "Trade", or {@link Optional#empty()} when there is none in the scene
     */
    public Optional<NpcEntity> nearestShopkeeper() {
        return shopkeepers().first();
    }

    /**
     * Opens the nearest shopkeeper's shop and waits for the interface.
     *
     * @return true when the shop is open when this returns
     */
    public boolean open() {
        return open(nearestShopkeeper().orElse(null));
    }

    /**
     * Opens the shop of the nearest shopkeeper with the given name.
     *
     * @param npcName the shopkeeper's name; case-insensitive and must be the full name
     * @return true when the shop is open when this returns
     */
    public boolean open(String npcName) {
        return open(shopkeepers().withName(npcName).first().orElse(null));
    }

    /**
     * Opens a specific shopkeeper's shop and waits for the interface.
     *
     * <p>Blocks for up to five seconds. Returns true immediately when a shop is already open, so it is
     * safe to call at the top of a loop.</p>
     *
     * @param shopkeeper the NPC to trade with; null is treated as a failure to find one
     * @return true when the shop is open when this returns
     */
    public boolean open(NpcEntity shopkeeper) {
        return open(shopkeeper, OPEN_TIMEOUT_MS);
    }

    /**
     * Opens a specific shopkeeper's shop and waits for the interface.
     *
     * @param shopkeeper the NPC to trade with; null is treated as a failure to find one
     * @param timeoutMs how long to wait for the interface, in milliseconds
     * @return true when the shop is open when this returns
     */
    public boolean open(NpcEntity shopkeeper, int timeoutMs) {
        if (isOpen()) {
            return true;
        }

        if (shopkeeper == null || shopkeeper.raw() == null) {
            log.warn("No shopkeeper found to trade with");
            return false;
        }

        if (!shopkeeper.interact(TRADE_ACTION)) {
            log.warn("Failed to dispatch Trade on {}", shopkeeper.getName());
            return false;
        }

        return SleepService.sleepUntil(this::isOpen, timeoutMs);
    }

    /**
     * Closes the shop interface and waits for it to go.
     *
     * <p>Clicks the interface's own close button when it can find one, and falls back to Escape, which
     * only works when the player has "Esc closes interfaces" enabled.</p>
     *
     * @return true when the shop is closed when this returns
     */
    public boolean close() {
        if (isClosed()) {
            return true;
        }

        Optional<WidgetEntity> closeButton = ctxProvider.get().widgets()
                .inGroup(InterfaceID.SHOPMAIN)
                .withAction("Close")
                .first();

        if (closeButton.isPresent()) {
            closeButton.get().interact("Close");
        } else {
            keyboard.keyPress(KeyEvent.VK_ESCAPE);
        }

        return SleepService.sleepUntil(this::isClosed, OPEN_TIMEOUT_MS);
    }

    /**
     * The quantity button currently selected in the shop interface.
     *
     * <p>Inferred from the shelves rather than read from a varbit, whose encoding is an undocumented
     * client detail: the first trade action on an item is the one a left click performs, and a left
     * click trades the selected quantity. Nothing else in this service depends on the selection, so a
     * null here is only ever a missing convenience.</p>
     *
     * @return the selected button, or null when no shop is open, the shelves are empty, or the
     *         selection could not be read
     */
    public ShopQuantity getQuantity() {
        if (isClosed()) {
            return null;
        }

        ShopEntity item = ctxProvider.get().shop().first().orElse(null);
        if (item == null) {
            return null;
        }

        for (String action : item.actions()) {
            int quantity = ShopActions.quantityOf(action);
            if (quantity == ShopActions.VARIABLE_QUANTITY) {
                return ShopQuantity.X;
            }
            if (quantity > 0) {
                return ShopQuantity.fromAmount(quantity);
            }
        }
        return null;
    }

    /**
     * Selects one of the quantity buttons along the bottom of the shop interface.
     *
     * <p>Only affects what a plain left click trades. Every method on this service names the quantity
     * it wants explicitly, so this is for plugins that care about the state the interface is left in.
     * Use {@link #setXAmount(int)} to also fill in the amount behind {@link ShopQuantity#X}.</p>
     *
     * @param quantity the button to select
     * @return true when the button was clicked, false when no shop is open
     */
    public boolean setQuantity(ShopQuantity quantity) {
        if (isClosed() || quantity == null) {
            return false;
        }

        interactionManager.interact(quantity.getWidgetId(), -1, -1, 1);
        return true;
    }

    /**
     * Selects the X button and answers the number dialogue it opens.
     *
     * <p>Blocks for a game tick while the dialogue is dismissed, so call it off the client thread.</p>
     *
     * @param amount the number of items X should stand for
     * @return true when the amount was submitted, false when no shop is open
     */
    public boolean setXAmount(int amount) {
        if (isClosed() || amount < 1) {
            return false;
        }

        if(ctxProvider.get().getClient().isClientThread()) {
            throw new IllegalStateException("setXAmount may not be called on the client thread");
        }

        interactionManager.interact(ShopQuantity.X.getWidgetId(), -1, -1, 1);
        interactionManager.getWidgetPackets().queueResumeCount(amount);

        // The count is answered by packet, but the dialogue the click opened is still on screen and
        // would swallow the next interaction if it were left there.
        SleepService.sleepFor(1);
        UIService.closeNumberDialogue();
        return true;
    }

    /**
     * The raw value of the shop's quantity selection varbit.
     *
     * <p>Exposed for plugins that want the client's own view of the selection. Prefer
     * {@link #getQuantity()}, which reports the same thing without depending on the encoding.</p>
     *
     * @return the varbit value
     */
    public int getQuantityVarbit() {
        return ctxProvider.get().getVarbitValue(VarbitID.SHOP_QUANTITY);
    }

    /**
     * How many coins the player is carrying.
     *
     * @return the coin stack size, or 0 when the player has none
     */
    public int coins() {
        Context ctx = ctxProvider.get();
        return ctx.runOnClientThread(() -> ctx.inventory().withId(ItemID.COINS).stream()
                .mapToInt(item -> item.raw().getQuantity())
                .sum());
    }

    /**
     * Asks the shop what a single one of an item costs to buy.
     *
     * <p>Clicks "Value" and waits for the shop's reply, so it blocks for up to three seconds and must
     * be called off the client thread. The answer is the price of the next one, which climbs as stock
     * falls.</p>
     *
     * @param item the shelf item to value
     * @return the price in coins, or -1 when the item is gone or the shop did not answer in time
     */
    public int value(ShopEntity item) {
        if (item == null || item.raw() == null) {
            return -1;
        }
        return quote(item.getName(), false, () -> item.interact("Value"));
    }

    /**
     * Asks the shop what it would pay for a single one of an item the player is carrying.
     *
     * <p>Clicks "Value" and waits for the shop's reply, so it blocks for up to three seconds and must
     * be called off the client thread. The answer is what the shop pays for the next one, which falls
     * as the shop takes more of them.</p>
     *
     * @param item the inventory item to value
     * @return the price in coins, or -1 when the item is gone or the shop did not answer in time
     */
    public int value(ShopInventoryEntity item) {
        if (item == null || item.raw() == null) {
            return -1;
        }
        return quote(item.getName(), true, () -> item.interact("Value"));
    }

    /**
     * Asks the shop what a single one of an item costs to buy, by item id.
     *
     * @param itemId the item to value
     * @return the price in coins, or -1 when the shop does not stock it or did not answer in time
     */
    public int value(int itemId) {
        return value(ctxProvider.get().shop().withId(itemId).first().orElse(null));
    }

    /**
     * Asks the shop what a single one of an item costs to buy, by name.
     *
     * @param itemName the item to value; case-insensitive and must be the full name
     * @return the price in coins, or -1 when the shop does not stock it or did not answer in time
     */
    public int value(String itemName) {
        return value(ctxProvider.get().shop().withName(itemName).first().orElse(null));
    }

    /**
     * Starts a purchase, bounded by whatever limits are added to the returned order.
     *
     * @param itemId the item to buy
     * @return an order to configure and {@link BuyOrder#execute()}
     */
    public BuyOrder buy(int itemId) {
        return new BuyOrder(this, itemId, null);
    }

    /**
     * Starts a purchase, bounded by whatever limits are added to the returned order.
     *
     * @param itemName the item to buy; case-insensitive and must be the full name
     * @return an order to configure and {@link BuyOrder#execute()}
     */
    public BuyOrder buy(String itemName) {
        return new BuyOrder(this, ShopOrder.NO_LIMIT, itemName);
    }

    /**
     * Starts a sale, bounded by whatever limits are added to the returned order.
     *
     * @param itemId the item to sell
     * @return an order to configure and {@link SellOrder#execute()}
     */
    public SellOrder sell(int itemId) {
        return new SellOrder(this, itemId, null);
    }

    /**
     * Starts a sale, bounded by whatever limits are added to the returned order.
     *
     * @param itemName the item to sell; case-insensitive and must be the full name
     * @return an order to configure and {@link SellOrder#execute()}
     */
    public SellOrder sell(String itemName) {
        return new SellOrder(this, ShopOrder.NO_LIMIT, itemName);
    }

    /**
     * Buys a set number of an item, with no price or coin limits.
     *
     * <p>Shorthand for {@code buy(itemId).quantity(amount).execute()}.</p>
     *
     * @param itemId the item to buy
     * @param amount how many to buy
     * @return what was bought and why it stopped
     */
    public ShopTransaction buy(int itemId, int amount) {
        return buy(itemId).quantity(amount).execute();
    }

    /**
     * Buys a set number of an item, with no price or coin limits.
     *
     * @param itemName the item to buy; case-insensitive and must be the full name
     * @param amount how many to buy
     * @return what was bought and why it stopped
     */
    public ShopTransaction buy(String itemName, int amount) {
        return buy(itemName).quantity(amount).execute();
    }

    /**
     * Sells every one of an item the player is carrying, with no price limits.
     *
     * @param itemId the item to sell
     * @return what was sold and why it stopped
     */
    public ShopTransaction sellAll(int itemId) {
        return sell(itemId).execute();
    }

    /**
     * Sells every one of an item the player is carrying, with no price limits.
     *
     * @param itemName the item to sell; case-insensitive and must be the full name
     * @return what was sold and why it stopped
     */
    public ShopTransaction sellAll(String itemName) {
        return sell(itemName).execute();
    }

    /**
     * Buys an exact amount immediately, without limits or accounting.
     *
     * <p>Backs {@code ShopEntity.buy(int)}. Dispatches clicks and returns; it does not wait for the
     * items to arrive.</p>
     *
     * @param item the shelf item to buy
     * @param amount how many to buy
     * @return true when at least one buy interaction was dispatched
     */
    public boolean buyNow(ShopEntity item, int amount) {
        if (item == null || item.raw() == null || amount < 1) {
            return false;
        }
        return tradeNow(item.raw().getActions(), amount, item::interact);
    }

    /**
     * Sells an exact amount immediately, without limits or accounting.
     *
     * <p>Backs {@code ShopInventoryEntity.sell(int)}. Dispatches clicks and returns; it does not wait
     * for the coins to arrive.</p>
     *
     * @param item the inventory item to sell
     * @param amount how many to sell
     * @return true when at least one sell interaction was dispatched
     */
    public boolean sellNow(ShopInventoryEntity item, int amount) {
        if (item == null || item.raw() == null || item.raw().getWidget() == null || amount < 1) {
            return false;
        }
        return tradeNow(item.raw().getWidget().getActions(), amount, item::interact);
    }

    /**
     * Runs a configured order against the open shop.
     *
     * <p>Called by {@link ShopOrder#execute()}. Trades in steps, re-reading stock, price and the
     * player's coins between each one, and stops at the first limit that bites.</p>
     *
     * @param order the buy or sell order to run
     * @return what was traded and why it stopped
     */
    ShopTransaction execute(ShopOrder<?> order) {
        String name = order.getItemName();
        int itemId = order.getItemId();
        int traded = 0;
        int coinsMoved = 0;
        int unitPrice = -1;

        if (order.getQuantity() < 1) {
            return result(order, itemId, name, 0, 0, ShopStopReason.NOTHING_REQUESTED);
        }

        while (true) {
            if (isClosed()) {
                return result(order, itemId, name, traded, coinsMoved, ShopStopReason.SHOP_CLOSED);
            }

            if (traded >= order.getQuantity()) {
                return result(order, itemId, name, traded, coinsMoved, ShopStopReason.QUANTITY_REACHED);
            }

            Tradeable tradeable = locate(order);
            if (tradeable == null) {
                ShopStopReason reason = traded > 0
                        ? (order.isSelling() ? ShopStopReason.NOTHING_LEFT_TO_SELL : ShopStopReason.OUT_OF_STOCK)
                        : ShopStopReason.ITEM_NOT_FOUND;
                return result(order, itemId, name, traded, coinsMoved, reason);
            }

            // Learn the real names and ids the first time round, so a request by name still reports
            // an id and a request by id still reports a name.
            itemId = tradeable.itemId;
            name = tradeable.name;

            if (tradeable.available < 1) {
                return result(order, itemId, name, traded, coinsMoved,
                        order.isSelling() ? ShopStopReason.NOTHING_LEFT_TO_SELL : ShopStopReason.OUT_OF_STOCK);
            }

            if (!order.isSelling()) {
                ShopStopReason blocked = buyingBlocked(tradeable);
                if (blocked != null) {
                    return result(order, itemId, name, traded, coinsMoved, blocked);
                }
            }

            // A fresh quote is only worth its round trip when a limit depends on the price of the
            // next item specifically. Otherwise the price measured from the last step is both free
            // and exact for what it describes.
            if (order.isRevalue() || (unitPrice < 0 && needsPrice(order))) {
                int quoted = tradeable.value();
                if (quoted >= 0) {
                    unitPrice = quoted;
                }
            }

            if (order.hasPriceLimit() && unitPrice >= 0 && priceLimitReached(order, unitPrice)) {
                return result(order, itemId, name, traded, coinsMoved, ShopStopReason.PRICE_LIMIT_REACHED);
            }

            int[] steps = ShopActions.fixedQuantities(tradeable.actions());
            if (steps.length == 0) {
                return result(order, itemId, name, traded, coinsMoved, ShopStopReason.SHOP_WILL_NOT_TRADE);
            }

            int coinsLeft = coinBudgetRemaining(order, coinsMoved);
            if (order.hasCoinLimit() && coinsLeft <= 0) {
                return result(order, itemId, name, traded, coinsMoved, ShopStopReason.COIN_LIMIT_REACHED);
            }

            int wanted = affordableQuantity(order, tradeable, unitPrice, traded, coinsLeft);
            if (wanted < 1) {
                return result(order, itemId, name, traded, coinsMoved, order.isSelling()
                        ? ShopStopReason.NO_PROGRESS
                        : (order.hasCoinLimit() ? ShopStopReason.COIN_LIMIT_REACHED : ShopStopReason.OUT_OF_COINS));
            }

            // Rounding down to a quantity the shop offers is what keeps the order from overshooting.
            int step = ShopActions.largestNotExceeding(steps, wanted);
            if (step < 1) {
                return result(order, itemId, name, traded, coinsMoved, ShopStopReason.STEP_TOO_LARGE);
            }

            StepOutcome outcome = tradeStep(order, tradeable, step);
            if (outcome.items < 1) {
                return result(order, itemId, name, traded, coinsMoved, ShopStopReason.NO_PROGRESS);
            }

            traded += outcome.items;
            coinsMoved += outcome.coins;

            // The coins that moved divided by the items that moved is the exact average price of the
            // step just made, which is the best available estimate for the next one. Shops that trade
            // in tokens move no coins, so their price stays whatever was last quoted.
            if (outcome.coins > 0) {
                unitPrice = Math.max(1, outcome.coins / outcome.items);
            }
        }
    }

    /**
     * Dispatches the clicks that trade an exact amount using the actions a widget offers.
     *
     * @param actions the widget's action array
     * @param amount how many items to trade
     * @param dispatch how to fire one action
     * @return true when at least one action was dispatched
     */
    private boolean tradeNow(String[] actions, int amount, Predicate<String> dispatch) {
        String exact = ShopActions.actionFor(actions, amount);
        if (exact != null) {
            return dispatch.test(exact);
        }

        int[] quantities = ShopActions.fixedQuantities(actions);
        boolean dispatched = false;
        int remaining = amount;

        for (int step : ShopActions.plan(remaining, quantities)) {
            String action = ShopActions.actionFor(actions, step);
            if (action == null || !dispatch.test(action)) {
                break;
            }
            dispatched = true;
            remaining -= step;
            SleepService.sleepFor(1);
        }

        if (remaining > 0) {
            log.warn("Shop offers no action combination for {} items, {} left untraded", amount, remaining);
        }
        return dispatched;
    }

    /**
     * Clicks "Value" and waits for the shop to quote a price for the named item.
     *
     * @param itemName the item being valued, used to reject a quote for something else
     * @param sellPrice true when waiting for the shop's buying price rather than its selling price
     * @param dispatch fires the Value action
     * @return the quoted price, or -1 when nothing matching arrived in time
     */
    private int quote(String itemName, boolean sellPrice, Runnable dispatch) {
        if (itemName == null || isClosed()) {
            return -1;
        }

        // Clearing first means a quote left over from an earlier item can never be mistaken for this
        // one's, which matters because the two directions produce near identical messages.
        lastQuote.set(null);
        dispatch.run();

        boolean quoted = SleepService.sleepUntil(() -> matches(lastQuote.get(), itemName, sellPrice), VALUE_TIMEOUT_MS);
        if (!quoted) {
            log.debug("Shop did not quote a {} price for {} within {}ms",
                    sellPrice ? "sell" : "buy", itemName, VALUE_TIMEOUT_MS);
            return -1;
        }

        ShopPrice price = lastQuote.get();
        return price == null ? -1 : price.getAmount();
    }

    /**
     * Whether a quote is the one being waited for.
     *
     * @param price the quote to test, may be null
     * @param itemName the item that was valued
     * @param sellPrice the direction that was asked for
     * @return true when the quote names the item and is for the right direction
     */
    private boolean matches(@Nullable ShopPrice price, String itemName, boolean sellPrice) {
        return price != null && price.isSellPrice() == sellPrice && price.isFor(itemName);
    }

    /**
     * Finds the item an order refers to, on whichever side of the shop it trades on.
     *
     * @param order the running order
     * @return the located item, or null when it is not there
     */
    private Tradeable locate(ShopOrder<?> order) {
        Context ctx = ctxProvider.get();

        if (order.isSelling()) {
            ShopInventoryEntity item = order.getItemName() == null
                    ? ctx.shopInventory().withId(order.getItemId()).first().orElse(null)
                    : ctx.shopInventory().withName(order.getItemName()).first().orElse(null);

            if (item == null) {
                return null;
            }

            // Stacked items sell out of one slot; unstacked ones occupy one slot each, so the whole
            // held amount is what is available either way.
            int held = ctx.shopInventory().withId(item.getId()).stream()
                    .mapToInt(ShopInventoryEntity::getQuantity)
                    .sum();

            return new Tradeable(item.getId(), item.getName(), held, item.raw().getWidget(),
                    () -> value(item), action -> item.interact(action), isStackable(item.getId()));
        }

        ShopEntity item = order.getItemName() == null
                ? ctx.shop().withId(order.getItemId()).first().orElse(null)
                : ctx.shop().withName(order.getItemName()).first().orElse(null);

        if (item == null) {
            return null;
        }

        return new Tradeable(item.getId(), item.getName(), item.stock(), item.raw(),
                () -> value(item), action -> item.interact(action), isStackable(item.getId()));
    }

    /**
     * Checks the conditions that stop a purchase before it is attempted.
     *
     * @param tradeable the item about to be bought
     * @return the reason to stop, or null when the purchase can go ahead
     */
    private ShopStopReason buyingBlocked(Tradeable tradeable) {
        Context ctx = ctxProvider.get();

        if (coins() < 1) {
            return ShopStopReason.OUT_OF_COINS;
        }

        // A stack the player already holds grows in place; anything else needs a free slot.
        boolean needsFreeSlot = !tradeable.stackable || held(tradeable.itemId) < 1;

        if (needsFreeSlot && ctx.inventory().isFull()) {
            return ShopStopReason.INVENTORY_FULL;
        }
        return null;
    }

    /**
     * Whether an order needs a quoted price before its first step.
     *
     * @param order the running order
     * @return true when a limit cannot be enforced without knowing the price up front
     */
    private boolean needsPrice(ShopOrder<?> order) {
        return order.hasPriceLimit() || order.hasCoinLimit();
    }

    /**
     * Whether the per item price limit has been passed.
     *
     * @param order the running order
     * @param unitPrice the current price of one item
     * @return true when buying has become too expensive, or selling too cheap
     */
    private boolean priceLimitReached(ShopOrder<?> order, int unitPrice) {
        return order.isSelling()
                ? unitPrice < order.getUnitPriceLimit()
                : unitPrice > order.getUnitPriceLimit();
    }

    /**
     * How much of the coin limit is left.
     *
     * @param order the running order
     * @param coinsMoved the coins spent or earned so far
     * @return the remaining budget or target, or {@link Integer#MAX_VALUE} when there is no limit
     */
    private int coinBudgetRemaining(ShopOrder<?> order, int coinsMoved) {
        return order.hasCoinLimit() ? order.getCoinLimit() - coinsMoved : Integer.MAX_VALUE;
    }

    /**
     * How many items the order could trade next, before rounding to a quantity the shop offers.
     *
     * <p>The smallest of everything that bounds it: what is left of the request, what is available,
     * the order's own step size, and — when buying — what the remaining coins actually stretch to.</p>
     *
     * @param order the running order
     * @param tradeable the item being traded
     * @param unitPrice the current price of one item, or -1 when unknown
     * @param traded how many have been traded so far
     * @param coinsLeft the remaining coin budget
     * @return the number of items that could be traded next, or 0 when none can be
     */
    private int affordableQuantity(ShopOrder<?> order, Tradeable tradeable, int unitPrice, int traded, int coinsLeft) {
        int wanted = Math.min(order.getStep(), order.getQuantity() - traded);
        wanted = Math.min(wanted, tradeable.available);

        // A limit that depends on the price cannot be respected without knowing it, and the shop did
        // not quote one. Trade a single item to discover the price from what it costs, rather than
        // committing a full step to a price nobody has seen.
        if (unitPrice < 1 && (order.hasPriceLimit() || order.hasCoinLimit())) {
            wanted = Math.min(wanted, 1);
        }

        // Only a purchase is bounded by coins; a sale brings them in.
        if (!order.isSelling()) {
            int spendable = Math.min(coinsLeft, coins());
            if (unitPrice > 0) {
                wanted = Math.min(wanted, spendable / unitPrice);
            } else if (spendable < 1) {
                return 0;
            }
        }

        if (wanted < 1) {
            return 0;
        }

        int[] quantities = ShopActions.fixedQuantities(tradeable.actions());
        return ShopActions.largestNotExceeding(quantities, wanted);
    }

    /**
     * Trades one step and measures what actually moved.
     *
     * <p>Both figures are measured rather than assumed: the item count settles the quantity, which is
     * what shops that trade in tokens have instead of a coin delta, and the coin stack settles the
     * price, which is what makes the running total exact as the price drifts.</p>
     *
     * @param order the running order
     * @param tradeable the item being traded
     * @param step how many items to trade
     * @return what moved
     */
    private StepOutcome tradeStep(ShopOrder<?> order, Tradeable tradeable, int step) {
        String action = ShopActions.actionFor(tradeable.actions(), step);
        if (action == null) {
            log.warn("Shop no longer offers a {} action for {} items", order.isSelling() ? "sell" : "buy", step);
            return new StepOutcome(0, 0);
        }

        int coinsBefore = coins();
        int heldBefore = held(tradeable.itemId);

        if (!tradeable.dispatch(action)) {
            return new StepOutcome(0, 0);
        }

        // A trade is one server tick, but the interface and the inventory update independently, so
        // wait on the item count and take whatever the coins say at that moment.
        SleepService.sleepUntil(() -> held(tradeable.itemId) != heldBefore, TRADE_TIMEOUT_MS);

        int itemDelta = held(tradeable.itemId) - heldBefore;
        int coinDelta = coins() - coinsBefore;

        int items = order.isSelling() ? -itemDelta : itemDelta;
        int coins = order.isSelling() ? coinDelta : -coinDelta;

        // Coins are also an item: selling coins to a shop, or buying them, would double count.
        if (tradeable.itemId == ItemID.COINS) {
            coins = 0;
        }

        return new StepOutcome(Math.max(0, items), Math.max(0, coins));
    }

    /**
     * How many of an item the player is carrying, counting stack sizes.
     *
     * @param itemId the item to count
     * @return the total held
     */
    private int held(int itemId) {
        Context ctx = ctxProvider.get();
        return ctx.runOnClientThread(() -> ctx.inventory().withId(itemId).stream()
                .mapToInt(item -> item.raw().getQuantity())
                .sum());
    }

    /**
     * Whether an item stacks in the inventory.
     *
     * @param itemId the item to check
     * @return true when one free slot holds any number of them
     */
    private boolean isStackable(int itemId) {
        Context ctx = ctxProvider.get();
        return ctx.runOnClientThreadOptional(() -> ctx.getItemManager().getItemComposition(itemId))
                .map(composition -> composition.isStackable() || composition.getNote() == 799)
                .orElse(false);
    }

    /**
     * Builds the result of a finished order.
     *
     * @param order the order that ran
     * @param itemId the item traded
     * @param itemName the item's name
     * @param quantity how many were traded
     * @param coins the coins that moved
     * @param reason why it stopped
     * @return the transaction record
     */
    private ShopTransaction result(ShopOrder<?> order, int itemId, String itemName, int quantity, int coins,
                                   ShopStopReason reason) {
        ShopTransaction transaction = ShopTransaction.builder()
                .selling(order.isSelling())
                .itemId(itemId)
                .itemName(itemName == null ? "Unknown item" : itemName)
                .quantity(quantity)
                .coins(coins)
                .stopReason(reason)
                .build();

        if (reason.isExpected()) {
            log.debug("{}", transaction);
        } else {
            log.warn("{}", transaction);
        }
        return transaction;
    }

    /**
     * One side of a trade, resolved to whatever the order is working with this round.
     *
     * <p>Exists so the trade loop is written once: a shelf item and an inventory item differ only in
     * where their actions live, how many are available, and which way the coins go.</p>
     */
    private static final class Tradeable {
        private final int itemId;
        private final String name;
        private final int available;
        private final Widget widget;
        private final IntSupplier valuer;
        private final Predicate<String> dispatcher;
        private final boolean stackable;

        private Tradeable(int itemId, String name, int available, Widget widget, IntSupplier valuer,
                          Predicate<String> dispatcher, boolean stackable) {
            this.itemId = itemId;
            this.name = name;
            this.available = available;
            this.widget = widget;
            this.valuer = valuer;
            this.dispatcher = dispatcher;
            this.stackable = stackable;
        }

        private String[] actions() {
            return widget == null ? null : widget.getActions();
        }

        private int value() {
            return valuer.getAsInt();
        }

        private boolean dispatch(String action) {
            return dispatcher.test(action);
        }
    }

    /** What one trade step actually moved. */
    private static final class StepOutcome {
        private final int items;
        private final int coins;

        private StepOutcome(int items, int coins) {
            this.items = items;
            this.coins = coins;
        }
    }
}
