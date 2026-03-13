//[kraken-api](../../../index.md)/[com.kraken.api.core.script](../index.md)/[Script](index.md)/[onGameTick](on-game-tick.md)

# onGameTick

[Kraken API]\
fun [onGameTick](on-game-tick.md)(event: GameTick)

Handles actions to be executed on each game tick event while the script is running. 

 This method is triggered by the `GameTick` event, which occurs at consistent 0.6s intervals in the game. It coordinates the execution of the script's main logic by invoking the [loop](loop.md) method on a separate thread. 

### Key Behavior:

- Ensures that the script is running before proceeding. If `isRunning` is `false`, the method returns immediately.
- Skips execution if a previous `loop()` call is still in progress, indicated by the `future` object.
- Submits the `loop()` logic to an `executor` service for asynchronous execution.
- If a delay is set by the `loop()` method, the thread sleeps for the specified duration before proceeding.
- Gracefully handles and logs exceptions thrown during the loop execution.
- Cleans up thread-local resources by calling [dispose](../-runnable-task/dispose.md).

### Threading Model:

 The main game logic defined in [loop](loop.md) is executed asynchronously to avoid blocking the main game thread. This separation allows for the use of thread sleeps and other blocking operations within the loop's logic. 

#### Parameters

Kraken API

| | |
|---|---|
| event | an instance of `GameTick` representing a single tick of the game clock. It triggers all logic tied to the game's periodic updates. <br>Example Usage:<br>```kotlin public class ExampleScript extends Script {    @Override    public int loop() {        log.info("Executing game logic...");        // Perform actions such as pathfinding or combat        return 1000; // Delay in milliseconds before the next execution    }} ``` |
