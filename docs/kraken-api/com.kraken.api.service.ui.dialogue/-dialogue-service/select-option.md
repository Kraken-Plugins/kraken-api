//[kraken-api](../../../index.md)/[com.kraken.api.service.ui.dialogue](../index.md)/[DialogueService](index.md)/[selectOption](select-option.md)

# selectOption

[Kraken API]\
open fun [selectOption](select-option.md)(option: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html))

Selects a specific option in a dialog option group. 

This method is executed on the client thread to select the desired option in the user interface dialog by sending the appropriate packet to the server.

#### Parameters

Kraken API

| | |
|---|---|
| option | the index of the option to select, typically starting from 0 for the first option. |

[Kraken API]\
open fun [selectOption](select-option.md)(option: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Attempts to select an option from a dialog interface based on the provided option text. This method works on a client thread to interact with widget components and identify the dialog options available. 

This method checks if the dialog interface exists and contains child widgets, iterates over them to match the provided option text (case-insensitive), and selects the desired option if found.

#### Return

`true` if the option was successfully found and selected; `false` if the dialog interface does not exist, the dialog options are unavailable or empty, or if the desired option cannot be found.

#### Parameters

Kraken API

| | |
|---|---|
| option | the text of the option to be selected from the dialog. This is case-insensitive and matched against the text of the dialog options. |
