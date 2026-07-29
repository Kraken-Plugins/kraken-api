//[kraken-api](../../../index.md)/[com.kraken.api.service.grandexchange](../index.md)/[GrandExchangeService](index.md)/[queueBuyOrder](queue-buy-order.md)

# queueBuyOrder

[Kraken API]\
open fun [queueBuyOrder](queue-buy-order.md)(slot: [GrandExchangeSlot](../-grand-exchange-slot/index.md), itemId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), amount: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), price: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [GrandExchangeSlot](../-grand-exchange-slot/index.md)

Queues a buy offer in the grand exchange for a given item, the amount of the item to purchase, and a specific price point. Negative amounts are not supported.

#### Return

The GrandExchangeSlot object for the GE slot that was used to queue the buy order, or null if no slot is free or an error occurs.

#### Parameters

Kraken API

| | |
|---|---|
| slot | The Grand Exchange Slot to use for the transaction |
| itemId | The id of the item to purchase |
| amount | The amount of the item to purchase |
| price | The price the item should be purchased at |

[Kraken API]\
open fun [queueBuyOrder](queue-buy-order.md)(item: [InventoryEntity](../../com.kraken.api.query.container.inventory/-inventory-entity/index.md), amount: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), price: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [GrandExchangeSlot](../-grand-exchange-slot/index.md)

Queues a buy offer for all of a specific item in the first free Grand Exchange slot.

#### Return

The GrandExchangeSlot object for the GE slot that was used to queue the buy order, or null if no slot is free or an error occurs.

#### Parameters

Kraken API

| | |
|---|---|
| item | The item to purchase |
| amount | The amount of the item to purchase |
| price | The price the item should be purchased at |

[Kraken API]\
open fun [queueBuyOrder](queue-buy-order.md)(itemId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), amount: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), price: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [GrandExchangeSlot](../-grand-exchange-slot/index.md)

Queues a buy offer for all of a specific item in the first free Grand Exchange slot.

#### Return

The GrandExchangeSlot object for the GE Slot that was used to queue the buy order or null if no slot is free/error occurs.

#### Parameters

Kraken API

| | |
|---|---|
| itemId | The item id to purchase |
| amount | The amount of the item to purchase |
| price | The price the item should be purchased at |
