//[kraken-api](../../../index.md)/[com.kraken.api.query.container.inventory](../index.md)/[InventoryQuery](index.md)/[quantityGreaterThan](quantity-greater-than.md)

# quantityGreaterThan

[Kraken API]\
open fun [quantityGreaterThan](quantity-greater-than.md)(amount: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [InventoryQuery](index.md)

Filters by item quantity. This filter is strictly greater than i.e `ctx.inventory().nameContains("karambwanji").quantityGreaterThan(500);` will only return a `ContainerItem` when 501 Karambwanji's are present.

#### Return

InventoryQuery

#### Parameters

Kraken API

| | |
|---|---|
| amount | The amount of the stack to filter for. |
