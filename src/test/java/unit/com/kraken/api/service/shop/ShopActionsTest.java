package unit.com.kraken.api.service.shop;

import com.kraken.api.service.shop.ShopActions;
import com.kraken.api.service.shop.ShopQuantity;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the action parsing and step sizing that decides what a shop order clicks.
 *
 * <p>These are the parts of {@code ShopService} that can be wrong without the client noticing: a
 * misparsed action buys the wrong amount, and a step that overshoots spends coins on stock nobody
 * asked for and cannot be undone.</p>
 */
class ShopActionsTest {

    /** The action array an OSRS shop puts on an item on its shelves. */
    private static final String[] SHOP_ITEM_ACTIONS = {"Value", "Buy 1", "Buy 5", "Buy 10", "Buy 50"};

    @Test
    void readsQuantityFromBuyAndSellActions() {
        assertEquals(1, ShopActions.quantityOf("Buy 1"));
        assertEquals(10, ShopActions.quantityOf("Buy 10"));
        assertEquals(50, ShopActions.quantityOf("Sell 50"));
    }

    @Test
    void toleratesSpacingAndCaseVariantsOfTheSameAction() {
        // Hyphenated is how the bank spells its equivalents, so a shop revision using that form must
        // not silently stop matching.
        assertEquals(5, ShopActions.quantityOf("Buy-5"));
        assertEquals(5, ShopActions.quantityOf("buy 5"));
        assertEquals(5, ShopActions.quantityOf("BUY5"));
    }

    @Test
    void readsQuantityThroughColourTags() {
        assertEquals(10, ShopActions.quantityOf("<col=ff9040>Buy 10</col>"));
    }

    @Test
    void reportsNonTradeActionsAsSuch() {
        assertEquals(ShopActions.NOT_A_TRADE_ACTION, ShopActions.quantityOf("Value"));
        assertEquals(ShopActions.NOT_A_TRADE_ACTION, ShopActions.quantityOf("Examine"));
        assertEquals(ShopActions.NOT_A_TRADE_ACTION, ShopActions.quantityOf(null));
        assertEquals(ShopActions.NOT_A_TRADE_ACTION, ShopActions.quantityOf(""));
    }

    @Test
    void reportsPlayerEnteredAmountsSeparatelyFromFixedOnes() {
        // "Buy X" needs a number dialogue, so it must never be mistaken for a fixed quantity.
        assertEquals(ShopActions.VARIABLE_QUANTITY, ShopActions.quantityOf("Buy X"));
        assertEquals(ShopActions.VARIABLE_QUANTITY, ShopActions.quantityOf("Sell x"));
    }

    @Test
    void listsFixedQuantitiesLargestFirst() {
        assertArrayEquals(new int[]{50, 10, 5, 1}, ShopActions.fixedQuantities(SHOP_ITEM_ACTIONS));
    }

    @Test
    void ignoresNullsAndDuplicatesWhenListingQuantities() {
        String[] actions = {"Value", null, "Buy 1", "Buy 1", null, "Buy X"};

        assertArrayEquals(new int[]{1}, ShopActions.fixedQuantities(actions));
    }

    @Test
    void listsNoQuantitiesForAnItemTheShopWillNotTrade() {
        // How a shop says no: it simply offers no sell action on an item it does not deal in.
        assertArrayEquals(new int[0], ShopActions.fixedQuantities(new String[]{"Value", "Examine"}));
        assertArrayEquals(new int[0], ShopActions.fixedQuantities(null));
    }

    @Test
    void returnsTheLiteralActionStringForAQuantity() {
        // The dispatcher compares for equality, so the spelling has to come back from the widget.
        assertEquals("Buy 10", ShopActions.actionFor(SHOP_ITEM_ACTIONS, 10));
        assertNull(ShopActions.actionFor(SHOP_ITEM_ACTIONS, 25));
    }

    @Test
    void findsThePlayerEnteredAmountAction() {
        assertEquals("Buy X", ShopActions.variableAction(new String[]{"Value", "Buy 1", "Buy X"}));
        assertNull(ShopActions.variableAction(SHOP_ITEM_ACTIONS));
    }

    @Test
    void picksTheLargestStepThatDoesNotOvershoot() {
        int[] quantities = {50, 10, 5, 1};

        assertEquals(50, ShopActions.largestNotExceeding(quantities, 50));
        assertEquals(10, ShopActions.largestNotExceeding(quantities, 37));
        assertEquals(5, ShopActions.largestNotExceeding(quantities, 9));
        assertEquals(1, ShopActions.largestNotExceeding(quantities, 1));
    }

    @Test
    void picksNoStepWhenNothingFits() {
        assertEquals(0, ShopActions.largestNotExceeding(new int[]{5, 10}, 4));
        assertEquals(0, ShopActions.largestNotExceeding(new int[]{1}, 0));
        assertEquals(0, ShopActions.largestNotExceeding(null, 10));
    }

    @Test
    void plansAnAwkwardTotalAsLargestStepsFirst() {
        assertEquals(Arrays.asList(10, 10, 10, 5, 1, 1), ShopActions.plan(37, new int[]{50, 10, 5, 1}));
    }

    @Test
    void planNeverAddsUpToMoreThanRequested() {
        int[] quantities = {50, 10, 5, 1};

        for (int total = 1; total <= 120; total++) {
            int planned = ShopActions.plan(total, quantities).stream().mapToInt(Integer::intValue).sum();
            assertEquals(total, planned, "plan for " + total + " must trade exactly that many");
        }
    }

    @Test
    void planStopsShortRatherThanOvershootingWhenNoSmallStepExists() {
        // A shop offering only "Buy 5" cannot trade 3, and buying 5 instead is not an acceptable
        // substitute: it spends the player's coins on stock they did not ask for.
        assertEquals(Collections.singletonList(5), ShopActions.plan(8, new int[]{5}));
        assertTrue(ShopActions.plan(3, new int[]{5}).isEmpty());
    }

    @Test
    void quantityButtonsResolveFromAnArbitraryAmount() {
        assertEquals(ShopQuantity.FIFTY, ShopQuantity.fromAmount(60));
        assertEquals(ShopQuantity.TEN, ShopQuantity.fromAmount(37));
        assertEquals(ShopQuantity.FIVE, ShopQuantity.fromAmount(9));
        assertEquals(ShopQuantity.ONE, ShopQuantity.fromAmount(1));
        assertNull(ShopQuantity.fromAmount(0));
    }

    @Test
    void onlyTheXButtonIsVariable() {
        assertTrue(ShopQuantity.ONE.isFixed());
        assertTrue(ShopQuantity.FIFTY.isFixed());
        assertTrue(!ShopQuantity.X.isFixed());
    }
}
