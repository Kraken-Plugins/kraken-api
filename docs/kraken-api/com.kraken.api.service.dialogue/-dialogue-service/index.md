//[kraken-api](../../../index.md)/[com.kraken.api.service.dialogue](../index.md)/[DialogueService](index.md)

# DialogueService

[Kraken API]\
open class [DialogueService](index.md)

A service class intended for managing and interacting with various types of dialogues in the game client. 

The `DialogueService` class provides utility methods for detecting dialogues, selecting options, resuming dialogues, handling text inputs, and extracting dialogue properties such as options, headers, and message content.

Using this service, developers can interface with different dialogue widgets within the game client, enabling automated interaction, data extraction, and execution of player actions. The methods in this class operate on the client thread and ensure safe synchronization with the game's UI components.

## Constructors

| | |
|---|---|
| [DialogueService](-dialogue-service.md) | [Kraken API]<br>constructor() |

## Functions

| Name | Summary |
|---|---|
| [continueDialogue](continue-dialogue.md) | [Kraken API]<br>open fun [continueDialogue](continue-dialogue.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Attempts to continue an active dialogue in the game by interacting with various dialogue widgets. |
| [continueNumericDialogue](continue-numeric-dialogue.md) | [Kraken API]<br>open fun [continueNumericDialogue](continue-numeric-dialogue.md)(value: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html))<br>Resumes a numeric dialogue in the game by interacting with the appropriate widgets and invoking client-side scripts to continue the process. |
| [getDialogueHeader](get-dialogue-header.md) | [Kraken API]<br>open fun [getDialogueHeader](get-dialogue-header.md)(): [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)<br>Retrieves the dialogue header text currently displayed in the widget interface. |
| [getDialogueOptions](get-dialogue-options.md) | [Kraken API]<br>open fun [getDialogueOptions](get-dialogue-options.md)(): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;[String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)&gt;<br>Retrieves a list of dialogue options currently available in the dialogue interface. |
| [getDialogueText](get-dialogue-text.md) | [Kraken API]<br>open fun [getDialogueText](get-dialogue-text.md)(): [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)<br>Retrieves the text from the currently active dialogue widget if available. |
| [isDialoguePresent](is-dialogue-present.md) | [Kraken API]<br>open fun [isDialoguePresent](is-dialogue-present.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Checks if any type of dialogue is currently present in the game client. |
| [isOptionPresent](is-option-present.md) | [Kraken API]<br>open fun [isOptionPresent](is-option-present.md)(option: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Checks if a specific dialogue option is present in the list of available options. |
| [selectOption](select-option.md) | [Kraken API]<br>open fun [selectOption](select-option.md)(option: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html))<br>Selects a specific option in a dialog option group.<br>[Kraken API]<br>open fun [selectOption](select-option.md)(option: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Attempts to select an option from a dialog interface based on the provided option text. |
