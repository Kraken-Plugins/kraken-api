//[kraken-api](../../../index.md)/[com.kraken.api.service.util.price](../index.md)/[ItemPriceService](index.md)/[refreshAllPrices](refresh-all-prices.md)

# refreshAllPrices

[Kraken API]\
open fun [refreshAllPrices](refresh-all-prices.md)(userAgent: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html))

Asynchronously fetches prices for ALL items to populate the cache. Useful for plugin startup.

#### Parameters

Kraken API

| | |
|---|---|
| userAgent | A user agent sent to the OSRS Wiki to identify the application fetching data. This should NOT be the basic java user agent or contain information about your plugins or client as it is sent to the Wiki and likely inspected. |
