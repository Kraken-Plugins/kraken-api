//[kraken-api](../../../index.md)/[com.kraken.api.core.script.breakhandler](../index.md)/[BreakConditions](index.md)/[onMaterialDepleted](on-material-depleted.md)

# onMaterialDepleted

[Kraken API]\
open fun [onMaterialDepleted](on-material-depleted.md)(ctx: [Context](../../com.kraken.api/-context/index.md), itemId: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [BreakCondition](../-break-condition/index.md)

Breaks when items in the bank run out (e.g., materials).

#### Return

BreakCondition

#### Parameters

Kraken API

| | |
|---|---|
| ctx | The API game context |
| itemId | The item id to track. When this item is no longer present in the bank or your inventory, a break will be taken. |
