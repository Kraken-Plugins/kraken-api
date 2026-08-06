package com.kraken.api.service.shop;

import lombok.Value;
import net.runelite.client.util.Text;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A price the shop quoted for a single item, parsed out of the game message the "Value" action
 * produces.
 *
 * <p>Shop prices are not held anywhere the client can read. The server owns them, they move as stock
 * moves, and the only way to learn one is to ask: valuing an item in the shop answers
 * "Bronze dagger: currently costs 26 coins.", and valuing one in the inventory answers
 * "Bronze dagger: shop will buy for 10 coins.". This type is the parsed form of either.</p>
 *
 * <p>The currency is captured rather than assumed so shops that trade in tokkul or other tokens
 * report something meaningful. {@code ShopService} only spends and receives real coins, so a
 * non-coin shop still trades correctly but reports zero coins moved.</p>
 */
@Value
public class ShopPrice {

    private static final Pattern VALUE_MESSAGE = Pattern.compile(
            "^(?<name>.+?):\\s*(?<verb>currently costs|costs|shop will buy for|will buy for|shop will pay|sells for)"
                    + "\\s+(?<amount>[\\d,]+)\\s*(?<currency>[a-z][a-z' ]*?)?\\s*[.!]?$",
            Pattern.CASE_INSENSITIVE);

    /** The item the shop quoted, exactly as the message named it. */
    String itemName;

    /** How much a single one costs, in whatever {@link #currency} the shop trades in. */
    int amount;

    /** The currency named in the message, lower cased, e.g. "coins" or "tokkul". Never null. */
    String currency;

    /**
     * Which direction this price is for.
     *
     * <p>True when it is what the shop pays the player, i.e. the price of selling to it. False when it
     * is what the player pays the shop.</p>
     */
    boolean sellPrice;

    /**
     * Parses a shop value message.
     *
     * <p>Tolerant by design: it accepts any of the phrasings the game uses for a quote, strips colour
     * tags and thousands separators, and rejects anything else. A message it cannot read is simply not
     * a price, which is the correct outcome for the hundreds of unrelated messages that arrive while a
     * shop is open.</p>
     *
     * @param message a raw game message, with or without colour tags; may be null
     * @return the parsed price, or empty when the message is not a shop quote
     */
    public static Optional<ShopPrice> parse(String message) {
        if (message == null) {
            return Optional.empty();
        }

        Matcher matcher = VALUE_MESSAGE.matcher(Text.removeTags(message).trim());
        if (!matcher.matches()) {
            return Optional.empty();
        }

        int amount;
        try {
            amount = Integer.parseInt(matcher.group("amount").replace(",", ""));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }

        String currency = matcher.group("currency");
        String verb = matcher.group("verb").toLowerCase();

        // "buy for" and "pay" are the shop buying from the player, which is the player's sell price.
        boolean sellPrice = verb.contains("buy for") || verb.contains("pay") || verb.contains("sells for");

        return Optional.of(new ShopPrice(
                Text.removeTags(matcher.group("name")).trim(),
                amount,
                currency == null ? "" : currency.trim().toLowerCase(),
                sellPrice
        ));
    }

    /**
     * Whether this quote names the given item.
     *
     * @param name the item name to compare against, may be null
     * @return true when the names match ignoring case and colour tags
     */
    public boolean isFor(String name) {
        return name != null && itemName.equalsIgnoreCase(Text.removeTags(name).trim());
    }
}
