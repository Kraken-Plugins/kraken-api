//[kraken-api](../../../index.md)/[com.kraken.api.simulation.colosseum.live](../index.md)/[ColoExecutor](index.md)

# ColoExecutor

[Kraken API]\
class [ColoExecutor](index.md)

Executes a [ColoDecision](../../com.kraken.api.simulation.colosseum.plan/-colo-decision/index.md) through the Kraken API in priority order: prayer first (the most tick-critical action), then consumables (survival), then gear swaps, then the attack or movement click. Attack and movement are mutually exclusive per tick, mirroring both the engine model and the real client (a new click replaces the interaction).

## Constructors

| | |
|---|---|
| [ColoExecutor](-colo-executor.md) | [Kraken API]<br>constructor(ctx: [Context](../../com.kraken.api/-context/index.md), prayerService: [PrayerService](../../com.kraken.api.service.prayer/-prayer-service/index.md), movementService: [MovementService](../../com.kraken.api.service.movement/-movement-service/index.md))<br>Creates the executor. |

## Functions

| Name | Summary |
|---|---|
| [execute](execute.md) | [Kraken API]<br>open fun [execute](execute.md)(decision: [ColoDecision](../../com.kraken.api.simulation.colosseum.plan/-colo-decision/index.md), loadout: [LoadoutConfig](../../com.kraken.api.simulation.colosseum/-loadout-config/index.md)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Executes every component of the decision for this tick. |
