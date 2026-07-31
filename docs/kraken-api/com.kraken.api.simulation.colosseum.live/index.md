//[kraken-api](../../index.md)/[com.kraken.api.simulation.colosseum.live](index.md)

# Package-level declarations

## Types

| Name | Summary |
|---|---|
| [ColoCapture](-colo-capture/index.md) | [Kraken API]<br>class [ColoCapture](-colo-capture/index.md)<br>Builds a [ColoFrame](../com.kraken.api.simulation.colosseum/-colo-frame/index.md) + [ColoState](../com.kraken.api.simulation.colosseum/-colo-state/index.md) pair from the live client without mutating anything: the arena collision grid (anchored on the colosseum region), every tracked wave NPC with cooldown/charge estimates from the [ColoWaveTracker](-colo-wave-tracker/index.md), and the player's vitals, supplies and consumption timers. |
| [ColoExecutor](-colo-executor/index.md) | [Kraken API]<br>class [ColoExecutor](-colo-executor/index.md)<br>Executes a [ColoDecision](../com.kraken.api.simulation.colosseum.plan/-colo-decision/index.md) through the Kraken API in priority order: prayer first (the most tick-critical action), then consumables (survival), then gear swaps, then the attack or movement click. |
| [ColoWaveTracker](-colo-wave-tracker/index.md) | [Kraken API]<br>class [ColoWaveTracker](-colo-wave-tracker/index.md)<br>Persistent per-wave observation state that a single snapshot cannot provide: attack cooldowns (from observed attack animations), manticore charge states and patterns (from spot-anims), javelin special counters, the warband attack-cycle phase, and the wave tick. |
