//[lib](../../../index.md)/[com.kraken.api.input.mouse.strategy](../index.md)/[MouseMovementStrategy](index.md)/[loadLibrary](load-library.md)

# loadLibrary

[Kraken API]\
open fun [loadLibrary](load-library.md)(libraryName: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html))

Loads a specified library into the current `MouseMovementStrategy` instance. 

This method delegates the library loading process to the active movement strategy if it is of type `ReplayStrategyMoveable`. The actual loading operation is handled by the underlying `ReplayStrategyMoveable` object.

- If the active strategy is not an instance of `ReplayStrategyMoveable`, this method does nothing.
- If the specified library name is invalid or null, it may result in no operation or an error within the `ReplayStrategyMoveable` implementation.

#### Parameters

Kraken API

| | |
|---|---|
| libraryName | The name of the library to be loaded. This must be a valid, non-null string representing the name or path of the library to ensure successful loading. |
