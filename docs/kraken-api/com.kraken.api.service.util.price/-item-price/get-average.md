//[kraken-api](../../../index.md)/[com.kraken.api.service.util.price](../index.md)/[ItemPrice](index.md)/[getAverage](get-average.md)

# getAverage

[Kraken API]\
open fun [getAverage](get-average.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)

Calculates the average price for the item based on its high and low prices. 

The average is determined by finding the midpoint between the `low` and `high` prices. The formula used is `low + ((high - low) / 2)`, which avoids potential floating-point arithmetic and maintains integer precision.

#### Return

The average price for the item as an integer value.
