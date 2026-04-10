//[kraken-api](../../../index.md)/[com.kraken.api.service.ui.processing](../index.md)/[ProcessingService](index.md)/[processByIndex](process-by-index.md)

# processByIndex

[Kraken API]\
open fun [processByIndex](process-by-index.md)(action: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html), index: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html))

Confirms the selected index by resuming the specific widget and child interface associated with the provided index and the current multi-quantity value. 

This method executes on the client thread to ensure thread safety. It resolves the widget ID using a base interface ID and the provided `index`, and queues the corresponding &quot;resume/pause&quot; action packet using the multi-quantity value retrieved by `getAmount()`.

#### Parameters

Kraken API

| | |
|---|---|
| action | The action to perform. i.e. &quot;Cook&quot;, &quot;Make&quot;, &quot;Craft&quot;, &quot;Smelt&quot;, etc... |
| index | The index to confirm in the interface. This value determines the child component of the base widget that the action will target. |
