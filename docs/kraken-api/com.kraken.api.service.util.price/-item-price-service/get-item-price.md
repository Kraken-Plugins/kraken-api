//[kraken-api](../../../index.md)/[com.kraken.api.service.util.price](../index.md)/[ItemPriceService](index.md)/[getItemPrice](get-item-price.md)

# getItemPrice

[Kraken API]\
open fun [getItemPrice](get-item-price.md)(itemId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html), userAgent: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html), callback: [Consumer](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/function/Consumer.html)&lt;[ItemPrice](../-item-price/index.md)&gt;)

Retrieves the price for a specific item. 

 1. Checks the local cache. 2. If missing, performs a non blocking network request for that specific item. This is safe to use on or off the RuneLite client thread. 

#### Parameters

Kraken API

| | |
|---|---|
| itemId | The OSRS Item ID |
| userAgent | A user agent sent to the OSRS Wiki to identify the application fetching data. This should NOT be the basic java user agent or contain information about your plugins or client as it is sent to the Wiki and likely inspected. |
| callback | A functional interface for consuming the result of the asynchronous API call |

#### Throws

| | |
|---|---|
| [RuntimeException](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/RuntimeException.html) | if called on the main client thread (optional safety check you could add) |
