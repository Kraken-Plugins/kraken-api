//[kraken-api](../../../index.md)/[com.kraken.api.service.util.price](../index.md)/[ItemPriceService](index.md)/[getItemPriceSync](get-item-price-sync.md)

# getItemPriceSync

[Kraken API]\
open fun [getItemPriceSync](get-item-price-sync.md)(itemId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), userAgent: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)): [ItemPrice](../-item-price/index.md)

Retrieves the price information for a specific item synchronously. 

This method first checks the local cache for the item's price. If the price is already cached, it retrieves the price from the cache. Otherwise, it performs a blocking network request to fetch the item's price from the API.

Note: This method may block the calling thread while performing the network request. It should not be called on the client thread or any other thread where blocking operations are not allowed.

#### Return

An `ItemPrice` object containing the price details for the specified item, or `null` if the item's price data is not available, the network request fails, or an error occurs during response parsing.

#### Parameters

Kraken API

| | |
|---|---|
| itemId | The unique identifier for the item whose price is to be retrieved. This is typically the OSRS Item ID. |
| userAgent | A user agent string sent to the API to identify the application fetching the data. It should NOT be the default Java user agent or contain information about plugins or the client, as this is sent to the Wiki and may be inspected. |
