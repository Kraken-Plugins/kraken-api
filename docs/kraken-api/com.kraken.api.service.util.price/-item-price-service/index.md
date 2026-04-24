//[kraken-api](../../../index.md)/[com.kraken.api.service.util.price](../index.md)/[ItemPriceService](index.md)

# ItemPriceService

[Kraken API]\
open class [ItemPriceService](index.md)

## Constructors

| | |
|---|---|
| [ItemPriceService](-item-price-service.md) | [Kraken API]<br>constructor(okHttpClient: OkHttpClient, gson: Gson) |

## Functions

| Name | Summary |
|---|---|
| [getItemPrice](get-item-price.md) | [Kraken API]<br>open fun [getItemPrice](get-item-price.md)(itemId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), userAgent: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html), callback: [Consumer](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/function/Consumer.html)&lt;[ItemPrice](../-item-price/index.md)&gt;)<br>Retrieves the price for a specific item. |
| [getItemPriceSync](get-item-price-sync.md) | [Kraken API]<br>open fun [getItemPriceSync](get-item-price-sync.md)(itemId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), userAgent: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)): [ItemPrice](../-item-price/index.md)<br>Retrieves the price information for a specific item synchronously. |
| [refreshAllPrices](refresh-all-prices.md) | [Kraken API]<br>open fun [refreshAllPrices](refresh-all-prices.md)(userAgent: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html))<br>Asynchronously fetches prices for ALL items to populate the cache. |
