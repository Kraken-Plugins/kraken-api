# Shops

`ShopService` covers the whole round trip with an NPC shop: finding a shopkeeper, opening their shop,
reading the shelves, and buying or selling under limits.

The work is split the same way banking is:

| Layer                                          | What it owns                                                                               |
|------------------------------------------------|--------------------------------------------------------------------------------------------|
| `ShopService` (`service.shop`)                 | The interface itself — open, close, quantity buttons — plus prices and the buy/sell engine |
| `ctx.shop()` (`query.container.shop`)          | Filtering the items on the shelves                                                         |
| `ctx.shopInventory()` (`query.container.shop`) | Filtering the player's inventory *as the shop draws it*, which is what you sell            |

`ctx.shopInventory()` is to `ctx.inventory()` what `ctx.bankInventory()` is to it while banking: the
shop renders the inventory in its own side panel with its own widgets, so a sell has to target those.

## Finding a shopkeeper

A shopkeeper is any NPC offering the `Trade` action, which is how the game itself marks them. That
covers general stores, the sawmill operator and a Slayer master's shop without knowing any names.

```java
ShopService shop = ctx.getService(ShopService.class);

shop.shopkeepers();                       // NpcQuery, nearest first — filter it like any other
shop.shopkeepers().within(10).first();
shop.nearestShopkeeper();

shop.open();                              // nearest shopkeeper, waits for the interface
shop.open("Shop keeper");                 // by name
shop.open(someNpcEntity, 8000);           // explicit NPC and timeout
shop.isOpen();
shop.close();
```

## Reading the shelves

```java
ctx.shop().list();                        // everything the shop deals in
ctx.shop().inStock().list();              // ...that it can actually sell right now
ctx.shop().withName("Iron ore").first();
ctx.shop().nameContains("rune").withMinimumStock(100).list();
ctx.shop().sortedByStock().first();       // deepest stock, so usually the cheapest
ctx.shop().stocks("Pot");
```

Sold out items stay on the shelves at a stock of zero so the shop can restock, so `inStock()` is the
filter that separates "this shop deals in it" from "this shop has it".

A `ShopEntity` exposes `stock()`, `slot()`, `actions()`, `buyQuantities()`, `value()` and
`buy(int)`/`buyOne()`/`buyFive()`/`buyTen()`/`buyFifty()`. `ShopInventoryEntity` mirrors it with
`count()`, `sellQuantities()`, `isSellable()`, `value()` and `sell(int)`.

`buy(int)` and `sell(int)` are the low level form: they dispatch clicks and return without waiting or
accounting. Use an order when the trade has to respect a limit.

## Prices

**A shop's prices are the server's, not the client's.** Nothing in the client holds them, and they
move with every item traded — each one bought costs a little more than the last, each one sold pays a
little less. There are exactly two ways to learn one, and the service uses both:

- **Ask.** `value(...)` clicks "Value" and parses the reply ("Iron ore: currently costs 45 coins.").
  Exact, and describes the *next* item, but costs a server round trip.
- **Measure.** During an order, the player's coin stack is read before and after every step. Free and
  exact, but describes the step just completed rather than the next one.

Orders run on the measured price and only pay for a quote when they have no measurement yet, or when
told to `revalue(true)`.

This degrades gracefully. If the shop never answers a quote — an unfamiliar message format, a shop
that does not support "Value" — an order with a price limit trades a **single** item first, reads what
it cost from the coin stack, and enforces the limit from there. So a price limit can let exactly one
item through before it starts to hold, and only when no quote was available.

```java
shop.value("Iron ore");                            // what the shop charges for the next one
ctx.shopInventory().withName("Bones").first().value();  // what the shop pays for the next one
shop.coins();
```

Both block for up to three seconds waiting for the shop to answer, and return `-1` if it does not.

## Buying and selling under limits

A shop's price moving as its stock moves is why "buy 500 iron ore" and "buy iron ore while it stays
under 30 gp" are different requests, and why most plugins want both at once. An order takes as many
limits as apply and stops at the first one to bite.

```java
ShopTransaction bought = shop.buy("Iron ore")
        .quantity(500)         // never more than 500
        .maxPrice(30)          // stop once the next one would cost more than 30
        .maxSpend(10_000)      // and never spend more than 10k in total
        .execute();

ShopTransaction sold = shop.sell("Bones")
        .quantity(200)
        .minPrice(20)          // stop once the shop pays less than 20 each
        .targetEarnings(50_000)
        .execute();

// Shorthands for the unlimited cases
shop.buy("Pot", 10);
shop.sellAll("Bones");
```

`ShopTransaction` reports what actually happened: `getQuantity()`, `getCoins()`, `getAveragePrice()`,
`isComplete()` and `getStopReason()`. The stop reason is the point of it — a plugin that asked for 500
and got 120 needs to know whether the shop ran dry (`OUT_OF_STOCK`), the price climbed past the limit
(`PRICE_LIMIT_REACHED`), or the budget ran out (`COIN_LIMIT_REACHED`), because each calls for a
different next move. `SHOP_WILL_NOT_TRADE` is the shop refusing the item outright, which it signals by
offering no trade action at all.

### How an order trades

Each round of the loop re-reads stock, the player's coins and the price, then trades the largest step
that fits inside every limit — the order's own `step`, what is left of the request, what is in stock,
and what the remaining budget stretches to. It then rounds *down* to a quantity the shop offers, so a
step never overshoots and never spends coins on stock nobody asked for.

Two knobs control the trade off:

- `step(int)` — the most items per click, default 50 (the largest single trade a shop offers). Limits
  are only re-checked between steps, so the step size is also the granularity at which they hold.
  `step(1)` enforces a price limit to the exact item, at one click per item.
- `revalue(boolean)` — ask the shop for a fresh quote before every step. Off by default. Turn it on
  when the order must react to the price of the *next* item rather than the average of the last step.

The quantities available are read back from the widget every time rather than assumed, so a client
revision that reorders or renames a "Buy" option changes what the service reports, not whether it
works. When a shop offers no action for the exact amount wanted, the amount is broken into the largest
steps it does offer: 37 becomes 10, 10, 10, 5, 1, 1.

### Shops that do not trade in coins

TzHaar shops and their kin quote tokkul. Quantities still work exactly as above, and `ShopPrice`
reports the currency it was quoted in, but `ShopTransaction.getCoins()` only ever counts real coins —
so a token shop reports the quantity it moved and zero coins. Coin based limits mean nothing there;
bound those orders by quantity.

## Quantity buttons

Every method here names the quantity it wants explicitly, so the buttons along the bottom of the
interface only matter to plugins that care what state the interface is left in.

```java
shop.setQuantity(ShopQuantity.TEN);
shop.setXAmount(250);       // selects X and answers the number dialogue
shop.getQuantity();         // inferred from the shelves, not from a varbit
```

## Threading

Anything that waits for the game — `open`, `close`, `value`, `setXAmount` and any order — blocks, so
call it off the client thread, from a `Script` loop or a worker. The query accessors and the plain
state checks are safe anywhere.

## Testing

`ShopServiceTest` in the in-client harness runs the whole path against the Varrock general store: it
opens the shop, quotes a price, proves an unmeetable price limit stops the order before it spends
anything, buys two pots inside a budget and sells them back. Its preconditions are in
[TESTS.md](TESTS.md). The parsing and step sizing underneath it are covered by ordinary unit tests in
`unit/com/kraken/api/service/shop/`.
