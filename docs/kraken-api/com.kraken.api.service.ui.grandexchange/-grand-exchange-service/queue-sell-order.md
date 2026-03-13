//[kraken-api](../../../index.md)/[com.kraken.api.service.ui.grandexchange](../index.md)/[GrandExchangeService](index.md)/[queueSellOrder](queue-sell-order.md)

# queueSellOrder

[Kraken API]\
open fun [queueSellOrder](queue-sell-order.md)(item: [InventoryEntity](../../com.kraken.api.query.container.inventory/-inventory-entity/index.md), amount: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), price: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [GrandExchangeSlot](../-grand-exchange-slot/index.md)

Queues a sell offer in the first free Grand Exchange slot.

#### Return

The GrandExchangeSlot object for the GE slot that was used to queue the sell order, or null if no slot is free or an error occurs.

#### Parameters

Kraken API

| | |
|---|---|
| item | The item to sell. |
| amount | The amount to sell. Use -1 to sell all available. |
| price | The price per item. |

[Kraken API]\
open fun [queueSellOrder](queue-sell-order.md)(itemId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), amount: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), price: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [GrandExchangeSlot](../-grand-exchange-slot/index.md)

Queues a sell offer in the first free Grand Exchange slot. When the amount specified is -1 then all of that item will be sold as part of the offer.

#### Return

The GrandExchangeSlot object for the GE slot that was used to queue the sell order, or null if no slot is free or an error occurs.

#### Parameters

Kraken API

| | |
|---|---|
| itemId | The item id of the item to sell. |
| amount | The amount to sell. Use -1 to sell all available. |
| price | The price per item. |

[Kraken API]\
open fun [queueSellOrder](queue-sell-order.md)(itemId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), price: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [GrandExchangeSlot](../-grand-exchange-slot/index.md)

Queues a sell offer for all of a specific item in the first free Grand Exchange slot.

#### Return

The GrandExchangeSlot object for the GE slot that was used to queue the sell order, or null if no slot is free or an error occurs.

#### Parameters

Kraken API

| | |
|---|---|
| itemId | The item id to sell |
| price | The price to sell the item for. |
