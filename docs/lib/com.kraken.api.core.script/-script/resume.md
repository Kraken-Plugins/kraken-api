//[lib](../../../index.md)/[com.kraken.api.core.script](../index.md)/[Script](index.md)/[resume](resume.md)

# resume

[Kraken API]\
fun [resume](resume.md)()

Resumes the execution of the script if it is currently paused. 

 This method transitions the script's state to running by setting the internal `isRunning` flag to `true`. During this process, a log entry is generated to indicate that the script has been resumed. If the script is already running, this method does nothing. 

### Behavior

- If the script is paused (`isRunning == false`), the method sets `isRunning` to `true` and logs the resumption.
- If the script is already running, the method performs no actions.

### Thread-Safety

Ensure thread-safe access to the script's state before calling this method.

### Example Usage:

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
