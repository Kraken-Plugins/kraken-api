//[kraken-api](../../../index.md)/[com.kraken.api.core.script](../index.md)/[Script](index.md)/[start](start.md)

# start

[Kraken API]\
fun [start](start.md)()

Starts the script execution, initializing the necessary parts and marking the script as running. 

 This method transitions the script into a &quot;running&quot; state by performing the following steps: 

- Verifies if the script is already running; if so, the method returns immediately.
- Sets the internal `isRunning` flag to `true` to indicate that the script is running.
- Registers the script instance to the `eventBus` for event handling.
- Generates a log entry indicating that the script has started.
- Invokes the [onStart](on-start.md) method to allow subclasses to define custom startup logic.

### Thread-Safety

 This method ensures thread-safe initialization of the script's state. However, external synchronization may be required if the method is invoked from multiple threads. 

### Behavior

- If the script is already running, no further actions are performed.
- Otherwise, the script is initialized, event handling is enabled, and startup logic is executed.

### Example Usage

Used during the initialization process of a script:

```kotlin

// Called during the plugin's start-up phase to launch the script
@Override
protected void startUp() {
    context.register();
    context.initializePackets();
    exampleScript.start(); // Start the script

    overlayManager.add(overlay);
}

```
