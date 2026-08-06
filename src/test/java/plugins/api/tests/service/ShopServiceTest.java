package plugins.api.tests.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.query.container.shop.ShopEntity;
import com.kraken.api.query.npc.NpcEntity;
import com.kraken.api.service.shop.ShopStopReason;
import com.kraken.api.service.shop.ShopService;
import com.kraken.api.service.shop.ShopTransaction;
import com.kraken.api.service.util.SleepService;
import lombok.extern.slf4j.Slf4j;
import plugins.api.requirements.BankState;
import plugins.api.requirements.ItemRequirement;
import plugins.api.requirements.SideEffect;
import plugins.api.requirements.TestRequirements;
import plugins.api.tests.BaseApiTest;
import plugins.api.world.Facility;
import plugins.api.world.NamedLocation;

/**
 * Exercises {@link ShopService} against the Varrock general store.
 *
 * <p>Buys two pots and sells them straight back, which costs a handful of coins and leaves the world
 * as it found it. The general store is used because it always stocks pots, always has enough of them,
 * and buys them back, so the test does not depend on another player's trading.</p>
 */
@Slf4j
@Singleton
public class ShopServiceTest extends BaseApiTest {

    private static final String ITEM = "Pot";
    private static final int AMOUNT = 2;

    @Inject
    private Context ctx;

    @Inject
    private ShopService shopService;

    @Override
    public TestRequirements requirements() {
        // Destructive because buying and selling back pays the shop's spread, so the run costs a few
        // coins every time. Nothing else it does outlives the test.
        return TestRequirements.builder()
                .location(NamedLocation.VARROCK_GENERAL_STORE)
                .facility(Facility.SHOP_NPC)
                .bankState(BankState.CLOSED)
                .inventoryItem(ItemRequirement.of("Coins", 1000))
                .destructive(true)
                .sideEffect(SideEffect.CONSUMES_ITEMS)
                .sideEffect(SideEffect.MOVES_PLAYER)
                .build();
    }

    @Override
    protected boolean runTest(Context ctx) throws Exception {
        NpcEntity shopkeeper = shopService.nearestShopkeeper();
        if (!assertNotNull(shopkeeper, "A shopkeeper offering Trade is in the scene")) {
            return false;
        }

        if (!assertTrue(shopService.open(shopkeeper), "The shop interface opened")) {
            return false;
        }

        if (!assertTrue(ctx.shop().inStock().count() > 0, "The shop has stock on its shelves")) {
            return false;
        }

        ShopEntity item = ctx.shop().withName(ITEM).first();
        if (!assertNotNull(item, "The shop stocks " + ITEM)) {
            return false;
        }

        int stockBefore = item.stock();
        if (!assertTrue(stockBefore >= AMOUNT, "The shop has at least " + AMOUNT + " " + ITEM)) {
            return false;
        }

        int quotedPrice = item.value();
        if (!assertTrue(quotedPrice > 0, "The shop quoted a buy price for " + ITEM + ": " + quotedPrice)) {
            return false;
        }

        // A limit set below the quoted price must stop the order before it spends anything at all.
        ShopTransaction refused = shopService.buy(ITEM).quantity(1000).maxPrice(quotedPrice - 1).execute();
        boolean limitHeld = assertEquals(ShopStopReason.PRICE_LIMIT_REACHED, refused.getStopReason(),
                "An unmeetable price limit stops the order")
                && assertEquals(0, refused.getQuantity(), "Nothing is bought past a price limit")
                && assertEquals(0, refused.getCoins(), "No coins are spent past a price limit");

        int coinsBefore = shopService.coins();
        int heldBefore = held();

        ShopTransaction bought = shopService.buy(ITEM).quantity(AMOUNT).maxSpend(500).execute();
        log.info("Buy result: {}", bought);

        boolean buyWorked = assertTrue(bought.isComplete(), "Bought the full requested quantity")
                && assertEquals(AMOUNT, bought.getQuantity(), "Bought exactly " + AMOUNT + " " + ITEM)
                && assertTrue(bought.getCoins() > 0, "The purchase cost coins: " + bought.getCoins())
                && assertTrue(bought.getCoins() <= 500, "The purchase stayed inside its budget");

        SleepService.sleepFor(1);

        boolean arrived = assertEquals(heldBefore + AMOUNT, held(),
                "The bought items are in the inventory");
        boolean paid = assertTrue(shopService.coins() < coinsBefore, "The coins left the inventory");

        ShopTransaction sold = shopService.sell(ITEM).quantity(AMOUNT).execute();
        log.info("Sell result: {}", sold);

        boolean sellWorked = assertTrue(sold.isComplete(), "Sold the full requested quantity")
                && assertEquals(AMOUNT, sold.getQuantity(), "Sold exactly " + AMOUNT + " " + ITEM)
                && assertTrue(sold.getCoins() == 0, "The sale brought in coins: " + sold.getCoins()); // 0 coins bc it only costs 1 to buy

        boolean closed = assertTrue(shopService.close(), "The shop interface closed");

        return limitHeld && buyWorked && arrived && paid && sellWorked && closed;
    }

    /**
     * How many of the traded item the player is carrying.
     *
     * @return the stack size, or 0 when none are held
     */
    private int held() {
        return ctx.inventory().withName(ITEM).stream()
                .mapToInt(item -> item.raw().getQuantity())
                .sum();
    }

    @Override
    protected void onFinish() {
        shopService.close();
    }

    @Override
    public String getTestName() {
        return "Shop Service";
    }
}
