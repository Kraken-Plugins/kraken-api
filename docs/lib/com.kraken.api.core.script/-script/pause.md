//[lib](../../../index.md)/[com.kraken.api.core.script](../index.md)/[Script](index.md)/[pause](pause.md)

# pause

[Kraken API]\
fun [pause](pause.md)()

Pauses the execution of the script. 

 This method halts the script's execution by setting the internal `isRunning` flag to `false`. If the script is already paused, invoking this method will have no effect. A log entry is generated to indicate the transition to the paused state. 

### Behavior

- If the script is running (`isRunning == true`), the method sets `isRunning` to `false` and logs the pause action.
- If the script is already paused or not running, the method performs no actions.

### Thread-Safety

 Ensure thread-safe access to the script's state when invoking this method to prevent race conditions. 

### Example Usage

Used when a configuration change should trigger the script to pause:

```kotlin
@Subscribe
private void onConfigChanged(final ConfigChanged event) {
    if (event.getGroup().equals("testapi") and event.getKey().equalsIgnoreCase("pauseScript")) {
        if (config.pauseScript()) {
            exampleScript.pause();
        } else {
            exampleScript.resume();
        }
    }
}

```
