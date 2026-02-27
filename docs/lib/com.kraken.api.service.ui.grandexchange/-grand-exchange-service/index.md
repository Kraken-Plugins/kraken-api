//[lib](../../../index.md)/[com.kraken.api.service.ui.grandexchange](../index.md)/[GrandExchangeService](index.md)

# GrandExchangeService

[Kraken API]\
open class [GrandExchangeService](index.md)

## Constructors

| | |
|---|---|
| [GrandExchangeService](-grand-exchange-service.md) | [Kraken API]<br>constructor() |

## Functions

| Name | Summary |
|---|---|
| [cancelOffer](cancel-offer.md) | [Kraken API]<br>open fun [cancelOffer](cancel-offer.md)(slot: [GrandExchangeSlot](../-grand-exchange-slot/index.md))<br>Cancels an active Grand Exchange offer in the specified slot. |
| [collect](collect.md) | [Kraken API]<br>open fun [collect](collect.md)(slot: [GrandExchangeSlot](../-grand-exchange-slot/index.md), noted: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html))<br>Collects items from a canceled or completed Grand Exchange offer in the specified slot. |
| [collectAll](collect-all.md) | [Kraken API]<br>open fun [collectAll](collect-all.md)()<br>Collects all completed offers in the Grand Exchange. |
| [getFirstFreeSlot](get-first-free-slot.md) | [Kraken API]<br>open fun [getFirstFreeSlot](get-first-free-slot.md)(): [GrandExchangeSlot](../-grand-exchange-slot/index.md)<br>Attempts to find the first free GE Offer slot. |
| [isOfferDetailsOpen](is-offer-details-open.md) | [Kraken API]<br>open fun [isOfferDetailsOpen](is-offer-details-open.md)(itemId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Returns true when an offer details interface is open for a specific item |
| [isOpen](is-open.md) | [Kraken API]<br>open fun [isOpen](is-open.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Checks if the Grand Exchange interface is currently open. |
| [queueBuyOrder](queue-buy-order.md) | [Kraken API]<br>open fun [queueBuyOrder](queue-buy-order.md)(item: [InventoryEntity](../../com.kraken.api.query.container.inventory/-inventory-entity/index.md), amount: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), price: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [GrandExchangeSlot](../-grand-exchange-slot/index.md)<br>Queues a buy offer for all of a specific item in the first free Grand Exchange slot.<br>[Kraken API]<br>open fun [queueBuyOrder](queue-buy-order.md)(itemId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), amount: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), price: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [GrandExchangeSlot](../-grand-exchange-slot/index.md)<br>Queues a buy offer in the grand exchange for a given item, the amount of the item to purchase, and a specific price point. |
| [queueSellOrder](queue-sell-order.md) | [Kraken API]<br>open fun [queueSellOrder](queue-sell-order.md)(itemId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), price: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [GrandExchangeSlot](../-grand-exchange-slot/index.md)<br>Queues a sell offer for all of a specific item in the first free Grand Exchange slot.<br>[Kraken API]<br>open fun [queueSellOrder](queue-sell-order.md)(item: [InventoryEntity](../../com.kraken.api.query.container.inventory/-inventory-entity/index.md), amount: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), price: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [GrandExchangeSlot](../-grand-exchange-slot/index.md)<br>open fun [queueSellOrder](queue-sell-order.md)(itemId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), amount: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), price: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [GrandExchangeSlot](../-grand-exchange-slot/index.md)<br>Queues a sell offer in the first free Grand Exchange slot. |
