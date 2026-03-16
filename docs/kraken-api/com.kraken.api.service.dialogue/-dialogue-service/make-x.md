//[kraken-api](../../../index.md)/[com.kraken.api.service.dialogue](../index.md)/[DialogueService](index.md)/[makeX](make-x.md)

# makeX

[Kraken API]\
open fun [makeX](make-x.md)(quantity: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html))

Executes the &quot;Make X&quot; operation for a specified quantity. 

 This method interacts with the game's client thread to queue a resume/pause packet for completing a quantity-based action, such as creating multiple items in a crafting or production interface. 

 The operation is carried out by sending a specific widget interaction request using `widgetPackets.queueResumePause`. The widget ID used is hardcoded in the method and corresponds to a predefined interface element within the game. 

#### Parameters

Kraken API

| | |
|---|---|
| quantity | The number of items or actions to perform. Must be a positive integer representing the desired quantity for the &quot;Make X&quot; operation. |
