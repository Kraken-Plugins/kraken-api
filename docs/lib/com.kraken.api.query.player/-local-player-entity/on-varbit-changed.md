//[lib](../../../index.md)/[com.kraken.api.query.player](../index.md)/[LocalPlayerEntity](index.md)/[onVarbitChanged](on-varbit-changed.md)

# onVarbitChanged

[Kraken API]\
open fun [onVarbitChanged](on-varbit-changed.md)(event: VarbitChanged)

The poisoned status of the player, with negative values indicating the duration of poison or venom protection and positive values representing the amount of poison or venom damage the player will be taking.

#### Parameters

Kraken API

| | |
|---|---|
| event | The varbit changed event from RuneLite - (-inf, -38): Venom immune for a duration of `(abs(val) - 38) * 30` game ticks (18 seconds per poison tick), after which point the value will have increased to `-38` and be representing poison immunity rather than venom immunity - [-38, 0): Poison immune for a duration of `abs(val) * 30` game ticks (18 seconds per poison tick) - 0: Not poisoned or immune to poison - [1, 100]: Poisoned for an amount of `ceil(val / 5.0f)` - [1000000, inf): Venomed for an amount of `min(20, (val - 999997) * 2)`, that is, an amount starting at 6 damage, increasing by 2 each time the value increases by one, and capped at 20 |
