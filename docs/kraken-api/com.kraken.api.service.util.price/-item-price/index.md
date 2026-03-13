//[kraken-api](../../../index.md)/[com.kraken.api.service.util.price](../index.md)/[ItemPrice](index.md)

# ItemPrice

[Kraken API]\
open class [ItemPrice](index.md)

Represents price data for an item in Old School RuneScape (OSRS), including high and low prices along with their respective timestamps. 

This class is typically used as a data model for interfacing with price-related APIs or services operating within the OSRS ecosystem.

### Attributes:

- itemId: The unique identifier for the item.
- high: The highest price recorded for the item.
- low: The lowest price recorded for the item.
- highTimestamp: The timestamp (as a Unix epoch) when the high price was last recorded.
- lowTimestamp: The timestamp (as a Unix epoch) when the low price was last recorded.

The `ItemPrice` class leverages Lombok's `{@}Data` and `{@}Builder` annotations, which provide boilerplate code generation for getters, setters, builder pattern, and more.

This object is immutable when using the builder pattern, ensuring consistent and thread-safe data access across concurrent operations.

## Constructors

| | |
|---|---|
| [ItemPrice](-item-price.md) | [Kraken API]<br>constructor() |

## Functions

| Name | Summary |
|---|---|
| [getAverage](get-average.md) | [Kraken API]<br>open fun [getAverage](get-average.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)<br>Calculates the average price for the item based on its high and low prices. |
