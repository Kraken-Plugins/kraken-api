//[kraken-api](../../../index.md)/[com.kraken.api.simulation.colosseum.live](../index.md)/[ColoCapture](index.md)/[capture](capture.md)

# capture

[Kraken API]\
open fun [capture](capture.md)(ctx: [Context](../../com.kraken.api/-context/index.md), tracker: [ColoWaveTracker](../-colo-wave-tracker/index.md), loadout: [LoadoutConfig](../../com.kraken.api.simulation.colosseum/-loadout-config/index.md), currentGearSet: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [ColoCapture](index.md)

Captures the current game state on the client thread.

#### Return

capture, or null when not in a capturable state (no player/collision).

#### Parameters

Kraken API

| | |
|---|---|
| ctx | kraken context. |
| tracker | wave tracker fed by the hosting plugin. |
| loadout | player loadout configuration. |
| currentGearSet | gear set index the player currently wears. |
