//[kraken-api](../../../index.md)/[com.kraken.api.query.container.bank](../index.md)/[DepositBoxQuery](index.md)/[quantityGreaterThan](quantity-greater-than.md)

# quantityGreaterThan

[Kraken API]\
open fun [quantityGreaterThan](quantity-greater-than.md)(amount: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [DepositBoxQuery](index.md)

Filters the `DepositBoxQuery` to include only items with a quantity greater than the specified amount. 

This method refines the `DepositBoxQuery` by applying a filtering condition that selects items from the deposit box where their quantity exceeds the given value. The resulting query will only include items meeting this criteria. 

- If no items in the deposit box have a quantity greater than the provided amount, the query result will be empty.

#### Return

DepositBoxQuery An updated query instance including only items whose quantity exceeds the specified amount.

#### Parameters

Kraken API

| | |
|---|---|
| amount | The minimum quantity threshold for filtering items. <br>- Must be a non-negative integer. - If `amount` is zero, all items with a positive quantity will be included in the result. |
