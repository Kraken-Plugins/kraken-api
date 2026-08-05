package unit.com.kraken.api.service.shop;

import com.kraken.api.service.shop.ShopPrice;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers parsing the game messages the shop "Value" action produces.
 *
 * <p>This parser is the only way the client ever learns a shop price, so everything a price limit
 * does rests on it. Getting the direction wrong is the dangerous failure: reading the shop's buying
 * price as the player's would make a "don't pay more than 30" limit compare against the wrong
 * number.</p>
 */
class ShopPriceTest {

    @Test
    void readsTheBuyPriceQuote() {
        ShopPrice price = ShopPrice.parse("Bronze dagger: currently costs 26 coins.").orElseThrow();

        assertEquals("Bronze dagger", price.getItemName());
        assertEquals(26, price.getAmount());
        assertEquals("coins", price.getCurrency());
        assertFalse(price.isSellPrice(), "'currently costs' is what the player pays");
    }

    @Test
    void readsTheSellPriceQuote() {
        ShopPrice price = ShopPrice.parse("Bronze dagger: shop will buy for 10 coins.").orElseThrow();

        assertEquals("Bronze dagger", price.getItemName());
        assertEquals(10, price.getAmount());
        assertTrue(price.isSellPrice(), "'shop will buy for' is what the shop pays");
    }

    @Test
    void readsQuotesThroughColourTags() {
        ShopPrice price = ShopPrice.parse("<col=ef1020>Iron ore</col>: currently costs 45 coins.").orElseThrow();

        assertEquals("Iron ore", price.getItemName());
        assertEquals(45, price.getAmount());
    }

    @Test
    void readsThousandsSeparatedAmounts() {
        ShopPrice price = ShopPrice.parse("Rune platebody: currently costs 65,000 coins.").orElseThrow();

        assertEquals(65_000, price.getAmount());
    }

    @Test
    void readsShopsThatTradeInSomethingOtherThanCoins() {
        // TzHaar shops quote tokkul. The amount still drives the limits; the currency is reported so a
        // plugin can tell that no coins will move.
        ShopPrice price = ShopPrice.parse("Obsidian cape: currently costs 90,000 tokkul.").orElseThrow();

        assertEquals(90_000, price.getAmount());
        assertEquals("tokkul", price.getCurrency());
    }

    @Test
    void keepsItemNamesThatContainPunctuation() {
        ShopPrice price = ShopPrice.parse("Zamorak monk's robe: currently costs 30 coins.").orElseThrow();

        assertEquals("Zamorak monk's robe", price.getItemName());
    }

    @Test
    void ignoresMessagesThatAreNotQuotes() {
        // A shop session is full of unrelated chat, and every message is offered to this parser.
        assertFalse(ShopPrice.parse("You don't have enough coins.").isPresent());
        assertFalse(ShopPrice.parse("Bob: hello there").isPresent());
        assertFalse(ShopPrice.parse("The shop has run out of stock.").isPresent());
        assertFalse(ShopPrice.parse("").isPresent());
        assertFalse(ShopPrice.parse(null).isPresent());
    }

    @Test
    void matchesQuotesAgainstTheItemThatWasValued() {
        ShopPrice price = ShopPrice.parse("Iron ore: currently costs 45 coins.").orElseThrow();

        assertTrue(price.isFor("Iron ore"));
        assertTrue(price.isFor("iron ore"), "item names are compared case-insensitively");
        assertTrue(price.isFor("<col=ff9040>Iron ore</col>"), "widget names arrive tagged");
        assertFalse(price.isFor("Iron bar"), "a quote for another item must not be accepted");
        assertFalse(price.isFor(null));
    }

    @Test
    void rejectsAnAmountTooLargeToHold() {
        Optional<ShopPrice> price = ShopPrice.parse("Item: currently costs 99999999999 coins.");

        assertFalse(price.isPresent(), "an unparseable amount is not a usable price");
    }
}
