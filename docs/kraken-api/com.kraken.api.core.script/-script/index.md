//[kraken-api](../../../index.md)/[com.kraken.api.core.script](../index.md)/[Script](index.md)

# Script

[Kraken API]\
abstract class [Script](index.md) : [Scriptable](../-scriptable/index.md)

## Constructors

| | |
|---|---|
| [Script](-script.md) | [Kraken API]<br>constructor() |

## Functions

| Name | Summary |
|---|---|
| [loop](loop.md) | [Kraken API]<br>abstract fun [loop](loop.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)<br>Executes a specific loop logic and returns an integer result based on the implementation. |
| [onGameTick](on-game-tick.md) | [Kraken API]<br>fun [onGameTick](on-game-tick.md)(event: GameTick)<br>Handles actions to be executed on each game tick event while the script is running. |
| [onStart](on-start.md) | [Kraken API]<br>open fun [onStart](on-start.md)()<br>Optional: Called when the script starts. |
| [onStop](on-stop.md) | [Kraken API]<br>open fun [onStop](on-stop.md)()<br>Optional: Called when the script stops. |
| [pause](pause.md) | [Kraken API]<br>fun [pause](pause.md)()<br>Pauses the execution of the script. |
| [resume](resume.md) | [Kraken API]<br>fun [resume](resume.md)()<br>Resumes the execution of the script if it is currently paused. |
| [start](start.md) | [Kraken API]<br>fun [start](start.md)()<br>Starts the script execution, initializing the necessary parts and marking the script as running. |
| [stop](stop.md) | [Kraken API]<br>open fun [stop](stop.md)()<br>Gracefully stops a running asynchronous loop.<br>[Kraken API]<br>open fun [stop](stop.md)(callback: [Runnable](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Runnable.html))<br>Stops the current execution loop and performs the necessary cleanup. |
