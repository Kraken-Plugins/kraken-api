//[lib](../../../index.md)/[com.kraken.api.input.mouse](../index.md)/[MouseRecorder](index.md)

# MouseRecorder

[Kraken API]\
open class [MouseRecorder](index.md)

The `MouseRecorder` class is responsible for recording mouse gestures such as clicks, movements, and drags. It captures the sequence of mouse events, organizes them into gestures, and writes them to disk for later analysis. 

This class is designed as a singleton and interacts closely with the `MouseManager` to register and deregister itself as a listener for mouse events. The recorded gestures can be categorized using labels provided during the recording start, and the gestures are stored in JSON format in a designated directory. 

Some features of the `MouseRecorder` include: 

- Categorizing gestures with user-defined labels
- Buffering gestures in memory and flushing them to disk in batches
- Asynchronous writing to avoid blocking event-handling threads

### Thread-Safety

While most operations are single-threaded, writing gestures to the disk is executed asynchronously to ensure that mouse event processing is not blocked. Internal buffers for gesture storage are synchronized to ensure thread safety during flush operations.

## Constructors

| | |
|---|---|
| [MouseRecorder](-mouse-recorder.md) | [Kraken API]<br>constructor(mouseManager: MouseManager) |

## Functions

| Name | Summary |
|---|---|
| [mouseClicked](mouse-clicked.md) | [Kraken API]<br>open fun [mouseClicked](mouse-clicked.md)(e: [MouseEvent](https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/java/awt/event/MouseEvent.html)): [MouseEvent](https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/java/awt/event/MouseEvent.html) |
| [mouseDragged](mouse-dragged.md) | [Kraken API]<br>open fun [mouseDragged](mouse-dragged.md)(e: [MouseEvent](https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/java/awt/event/MouseEvent.html)): [MouseEvent](https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/java/awt/event/MouseEvent.html) |
| [mouseEntered](mouse-entered.md) | [Kraken API]<br>open fun [mouseEntered](mouse-entered.md)(e: [MouseEvent](https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/java/awt/event/MouseEvent.html)): [MouseEvent](https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/java/awt/event/MouseEvent.html) |
| [mouseExited](mouse-exited.md) | [Kraken API]<br>open fun [mouseExited](mouse-exited.md)(e: [MouseEvent](https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/java/awt/event/MouseEvent.html)): [MouseEvent](https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/java/awt/event/MouseEvent.html) |
| [mouseMoved](mouse-moved.md) | [Kraken API]<br>open fun [mouseMoved](mouse-moved.md)(e: [MouseEvent](https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/java/awt/event/MouseEvent.html)): [MouseEvent](https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/java/awt/event/MouseEvent.html) |
| [mousePressed](mouse-pressed.md) | [Kraken API]<br>open fun [mousePressed](mouse-pressed.md)(e: [MouseEvent](https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/java/awt/event/MouseEvent.html)): [MouseEvent](https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/java/awt/event/MouseEvent.html) |
| [mouseReleased](mouse-released.md) | [Kraken API]<br>open fun [mouseReleased](mouse-released.md)(e: [MouseEvent](https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/java/awt/event/MouseEvent.html)): [MouseEvent](https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/java/awt/event/MouseEvent.html) |
| [start](start.md) | [Kraken API]<br>open fun [start](start.md)(label: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html))<br>Starts recording mouse movements and gestures with the given label. |
| [stop](stop.md) | [Kraken API]<br>open fun [stop](stop.md)()<br>Stops the recording of mouse movements and gestures. |
