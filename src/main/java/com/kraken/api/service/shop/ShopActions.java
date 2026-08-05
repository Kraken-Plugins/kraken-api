package com.kraken.api.service.shop;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.runelite.client.util.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Stateless helpers for reading the trade actions off a shop or shop inventory item widget.
 *
 * <p>Nothing here assumes a particular action layout. The quantities a shop offers are read back from
 * the widget every time, so a client revision that reorders, adds or removes a "Buy" option changes
 * only what these methods report, never whether they work. The one thing callers must respect is that
 * an action is dispatched using the exact string the widget carries, since
 * {@code ActionResolver.matches} compares for equality rather than by prefix.</p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ShopActions {

    /** Returned by {@link #quantityOf(String)} for an action that is not a buy or sell. */
    public static final int NOT_A_TRADE_ACTION = -1;

    /** Returned by {@link #quantityOf(String)} for a player entered amount, i.e. "Buy X". */
    public static final int VARIABLE_QUANTITY = 0;

    private static final Pattern TRADE_ACTION = Pattern.compile(
            "^(?:buy|sell)[\\s-]*(\\d+|x)$", Pattern.CASE_INSENSITIVE);

    /**
     * Reads the number of items a trade action moves.
     *
     * @param action a widget action such as "Buy 10", "Sell-5", "Buy X" or "Value"
     * @return the quantity, {@link #VARIABLE_QUANTITY} for a player entered amount, or
     *         {@link #NOT_A_TRADE_ACTION} when the action does not buy or sell
     */
    public static int quantityOf(String action) {
        if (action == null) {
            return NOT_A_TRADE_ACTION;
        }

        // Tags are stripped as well as sanitized: an action is dispatched by its literal string, but
        // it is recognized by what it says.
        Matcher matcher = TRADE_ACTION.matcher(Text.removeTags(Text.sanitize(action)).trim());
        if (!matcher.matches()) {
            return NOT_A_TRADE_ACTION;
        }

        String quantity = matcher.group(1);
        if (quantity.equalsIgnoreCase("x")) {
            return VARIABLE_QUANTITY;
        }

        try {
            return Integer.parseInt(quantity);
        } catch (NumberFormatException e) {
            return NOT_A_TRADE_ACTION;
        }
    }

    /**
     * Every fixed trade quantity the widget offers, largest first and without duplicates.
     *
     * <p>The order matters: the trade loop always spends the largest step it can, so a request for 60
     * becomes one 50 and one 10 rather than sixty single clicks.</p>
     *
     * @param actions the widget's action array, may be null or contain nulls
     * @return the available quantities in descending order, empty when the widget offers none
     */
    public static int[] fixedQuantities(String[] actions) {
        if (actions == null) {
            return new int[0];
        }

        return Arrays.stream(actions)
                .mapToInt(ShopActions::quantityOf)
                .filter(quantity -> quantity > 0)
                .distinct()
                .boxed()
                .sorted(Comparator.reverseOrder())
                .mapToInt(Integer::intValue)
                .toArray();
    }

    /**
     * Finds the literal action string that trades exactly {@code quantity} items.
     *
     * @param actions the widget's action array, may be null or contain nulls
     * @param quantity the number of items the action must trade
     * @return the action exactly as the widget spells it, or null when the widget has no such action
     */
    public static String actionFor(String[] actions, int quantity) {
        if (actions == null) {
            return null;
        }

        for (String action : actions) {
            if (quantityOf(action) == quantity) {
                return action;
            }
        }
        return null;
    }

    /**
     * Finds the literal action string for a player entered amount, i.e. "Buy X" or "Sell X".
     *
     * @param actions the widget's action array, may be null or contain nulls
     * @return the action exactly as the widget spells it, or null when the widget has no such action
     */
    public static String variableAction(String[] actions) {
        return actionFor(actions, VARIABLE_QUANTITY);
    }

    /**
     * The largest available quantity that does not overshoot what is still wanted.
     *
     * <p>Overshooting is never acceptable: buying 50 when 3 were asked for spends the player's coins
     * on stock they did not want, and cannot be undone.</p>
     *
     * @param quantities the available quantities, in any order
     * @param wanted how many items are still wanted
     * @return the largest quantity that is at most {@code wanted}, or 0 when none fits
     */
    public static int largestNotExceeding(int[] quantities, int wanted) {
        int best = 0;
        if (quantities == null || wanted < 1) {
            return best;
        }

        for (int quantity : quantities) {
            if (quantity <= wanted && quantity > best) {
                best = quantity;
            }
        }
        return best;
    }

    /**
     * Breaks a total down into the sequence of clicks that trades it.
     *
     * <p>Purely informational — the trade loop re-plans after every step because prices, stock and the
     * player's coins all move underneath it — but it makes the click cost of a request predictable and
     * is the easiest way to test the step sizing in isolation.</p>
     *
     * @param total how many items to trade in all
     * @param quantities the available quantities, in any order
     * @return the individual trade sizes in the order they would be clicked. Stops early, returning a
     *         short plan, when the remainder is smaller than the smallest available quantity
     */
    public static List<Integer> plan(int total, int[] quantities) {
        List<Integer> steps = new ArrayList<>();
        int remaining = total;

        while (remaining > 0) {
            int step = largestNotExceeding(quantities, remaining);
            if (step <= 0) {
                break;
            }
            steps.add(step);
            remaining -= step;
        }

        return steps;
    }
}
