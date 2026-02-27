//[lib](../../../index.md)/[com.kraken.api.input.mouse](../index.md)/[MouseRecorder](index.md)/[stop](stop.md)

# stop

[Kraken API]\
open fun [stop](stop.md)()

Stops the recording of mouse movements and gestures. 

 This method halts the recording session, flushes any pending gestures to disk, and unregisters the mouse listener to stop capturing events. 

 Steps performed: 

- Checks if recording is active and proceeds only if it is.
- Flushes unsaved gestures to persistent storage.
- Unregisters the mouse listener to stop event monitoring.
- Logs the action completion with the associated label.
