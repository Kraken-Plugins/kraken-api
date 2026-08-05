package unit.com.kraken.api.service.shop;

import com.kraken.api.service.shop.BuyOrder;
import com.kraken.api.service.shop.SellOrder;
import com.kraken.api.service.shop.ShopOrder;
import com.kraken.api.service.shop.ShopService;
import com.kraken.api.service.shop.ShopStopReason;
import com.kraken.api.service.shop.ShopTransaction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers how an order records its limits and how a finished one reports itself.
 *
 * <p>{@link ShopService} uses field injection, so an un-injected instance is safe here: building an
 * order touches no injected field, and nothing in this test executes one.</p>
 */
class ShopOrderTest {

    private final ShopService shopService = new ShopService();

    @Test
    void anOrderWithNoLimitsIsBoundedOnlyByTheWorld() {
        BuyOrder order = shopService.buy("Iron ore");

        assertEquals(Integer.MAX_VALUE, order.getQuantity(), "an unlimited order runs until the shop stops it");
        assertFalse(order.hasPriceLimit());
        assertFalse(order.hasCoinLimit());
        assertEquals(50, order.getStep(), "the default step is the largest single trade a shop offers");
        assertFalse(order.isRevalue(), "prices are measured from coin deltas unless asked otherwise");
    }

    @Test
    void buyLimitsAreRecordedAgainstTheSharedFields() {
        BuyOrder order = shopService.buy("Iron ore")
                .maxPrice(30)
                .maxSpend(10_000)
                .quantity(500);

        assertEquals(30, order.getUnitPriceLimit());
        assertEquals(10_000, order.getCoinLimit());
        assertEquals(500, order.getQuantity());
        assertTrue(order.hasPriceLimit());
        assertTrue(order.hasCoinLimit());
        assertFalse(order.isSelling());
    }

    @Test
    void sellLimitsAreRecordedAgainstTheSameFields() {
        // The two directions share the storage and differ only in how the service reads it, so a
        // minimum and a maximum must land in the same place.
        SellOrder order = shopService.sell("Bones")
                .minPrice(20)
                .targetEarnings(50_000)
                .quantity(200);

        assertEquals(20, order.getUnitPriceLimit());
        assertEquals(50_000, order.getCoinLimit());
        assertEquals(200, order.getQuantity());
        assertTrue(order.isSelling());
    }

    @Test
    void anOrderByIdCarriesNoNameAndAnOrderByNameCarriesNoId() {
        assertEquals(440, shopService.buy(440).getItemId());
        assertEquals(null, shopService.buy(440).getItemName());
        assertEquals(ShopOrder.NO_LIMIT, shopService.buy("Iron ore").getItemId());
        assertEquals("Iron ore", shopService.buy("Iron ore").getItemName());
    }

    @Test
    void stepIsClampedToATradeableSize() {
        // A step of zero would make the trade loop spin without ever trading anything.
        assertEquals(1, shopService.buy("Iron ore").step(0).getStep());
        assertEquals(1, shopService.buy("Iron ore").step(-5).getStep());
        assertEquals(10, shopService.buy("Iron ore").step(10).getStep());
    }

    @Test
    void reportsTheAveragePricePaidAcrossTheWholeOrder() {
        // 50 bought for 1600 is 32 each on average, even though no single one cost that.
        ShopTransaction transaction = ShopTransaction.builder()
                .selling(false)
                .itemId(440)
                .itemName("Iron ore")
                .quantity(50)
                .coins(1600)
                .stopReason(ShopStopReason.QUANTITY_REACHED)
                .build();

        assertEquals(32.0, transaction.getAveragePrice(), 0.001);
        assertTrue(transaction.isComplete());
        assertTrue(transaction.isTraded());
    }

    @Test
    void reportsNoAveragePriceWhenNothingWasTraded() {
        ShopTransaction transaction = ShopTransaction.builder()
                .selling(false)
                .itemId(440)
                .itemName("Iron ore")
                .quantity(0)
                .coins(0)
                .stopReason(ShopStopReason.PRICE_LIMIT_REACHED)
                .build();

        assertEquals(0, transaction.getAveragePrice(), 0.001, "no division by a zero quantity");
        assertFalse(transaction.isTraded());
        assertFalse(transaction.isComplete(), "stopping at a limit is not completing the order");
    }

    @Test
    void onlyAnUnexpectedStopIsWorthWarningAbout() {
        // Drives the log level the service picks, and tells a plugin whether to retry or move on.
        assertTrue(ShopStopReason.PRICE_LIMIT_REACHED.isExpected());
        assertTrue(ShopStopReason.OUT_OF_STOCK.isExpected());
        assertTrue(ShopStopReason.SHOP_WILL_NOT_TRADE.isExpected());
        assertFalse(ShopStopReason.SHOP_CLOSED.isExpected());
        assertFalse(ShopStopReason.NO_PROGRESS.isExpected());
        assertFalse(ShopStopReason.ITEM_NOT_FOUND.isExpected());
    }
}
