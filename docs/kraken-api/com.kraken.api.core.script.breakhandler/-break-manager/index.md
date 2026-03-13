//[kraken-api](../../../index.md)/[com.kraken.api.core.script.breakhandler](../index.md)/[BreakManager](index.md)

# BreakManager

[Kraken API]\
open class [BreakManager](index.md)

## Constructors

| | |
|---|---|
| [BreakManager](-break-manager.md) | [Kraken API]<br>constructor() |

## Functions

| Name | Summary |
|---|---|
| [attachScript](attach-script.md) | [Kraken API]<br>open fun [attachScript](attach-script.md)(script: [Script](../../com.kraken.api.core.script/-script/index.md), profile: [BreakProfile](../-break-profile/index.md))<br>Attaches a script to the break handler with a specific profile. |
| [detachScript](detach-script.md) | [Kraken API]<br>open fun [detachScript](detach-script.md)()<br>Detaches the current script from the break handler. |
| [initialize](initialize.md) | [Kraken API]<br>open fun [initialize](initialize.md)()<br>Initializes the break handler and registers it to the event bus. |
| [isActive](is-active.md) | [Kraken API]<br>open fun [isActive](is-active.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Checks if the handler is currently managing a script. |
| [isOnBreak](is-on-break.md) | [Kraken API]<br>open fun [isOnBreak](is-on-break.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Checks if currently on break. |
| [onGameStateChanged](on-game-state-changed.md) | [Kraken API]<br>open fun [onGameStateChanged](on-game-state-changed.md)(event: GameStateChanged) |
| [onGameTick](on-game-tick.md) | [Kraken API]<br>open fun [onGameTick](on-game-tick.md)(event: GameTick) |
| [shutdown](shutdown.md) | [Kraken API]<br>open fun [shutdown](shutdown.md)()<br>Shuts down the break handler and cleans up resources. |
| [triggerBreak](trigger-break.md) | [Kraken API]<br>open fun [triggerBreak](trigger-break.md)(reason: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Manually triggers a break. |
