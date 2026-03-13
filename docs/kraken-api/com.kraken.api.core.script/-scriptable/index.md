//[kraken-api](../../../index.md)/[com.kraken.api.core.script](../index.md)/[Scriptable](index.md)

# Scriptable

interface [Scriptable](index.md)

Interface representing a script that can be executed. Implementations of this interface define the main loop of a script.

#### Inheritors

| |
|---|
| [Script](../-script/index.md) |

## Functions

| Name | Summary |
|---|---|
| [onStart](on-start.md) | [Kraken API]<br>abstract fun [onStart](on-start.md)() |
| [onStop](on-stop.md) | [Kraken API]<br>abstract fun [onStop](on-stop.md)() |
| [stop](stop.md) | [Kraken API]<br>abstract fun [stop](stop.md)(callback: [Runnable](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Runnable.html))<br>Gracefully stops a running asynchronous loop(). |
