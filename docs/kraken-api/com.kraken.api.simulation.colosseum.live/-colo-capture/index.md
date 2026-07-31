//[kraken-api](../../../index.md)/[com.kraken.api.simulation.colosseum.live](../index.md)/[ColoCapture](index.md)

# ColoCapture

[Kraken API]\
class [ColoCapture](index.md)

Builds a [ColoFrame](../../com.kraken.api.simulation.colosseum/-colo-frame/index.md) + [ColoState](../../com.kraken.api.simulation.colosseum/-colo-state/index.md) pair from the live client without mutating anything: the arena collision grid (anchored on the colosseum region), every tracked wave NPC with cooldown/charge estimates from the [ColoWaveTracker](../-colo-wave-tracker/index.md), and the player's vitals, supplies and consumption timers.

## Functions

| Name | Summary |
|---|---|
| [capture](capture.md) | [Kraken API]<br>open fun [capture](capture.md)(ctx: [Context](../../com.kraken.api/-context/index.md), tracker: [ColoWaveTracker](../-colo-wave-tracker/index.md), loadout: [LoadoutConfig](../../com.kraken.api.simulation.colosseum/-loadout-config/index.md), currentGearSet: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [ColoCapture](index.md)<br>Captures the current game state on the client thread. |
