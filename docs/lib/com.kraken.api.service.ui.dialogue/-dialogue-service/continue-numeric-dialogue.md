//[lib](../../../index.md)/[com.kraken.api.service.ui.dialogue](../index.md)/[DialogueService](index.md)/[continueNumericDialogue](continue-numeric-dialogue.md)

# continueNumericDialogue

[Kraken API]\
open fun [continueNumericDialogue](continue-numeric-dialogue.md)(value: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html))

Resumes a numeric dialogue in the game by interacting with the appropriate widgets and invoking client-side scripts to continue the process. 

 This method is designed to handle numeric input dialogues and ensure the correct actions are executed in response. It queues a RESUME_COUNTDIALOG packet with the provided value and checks for specific chatbox input widgets to determine if a client script needs to be executed. If the relevant widgets are found, it invokes a client script to resume the numeric dialogue. 

 The execution occurs on the client thread to maintain proper synchronization with the game's UI and ensure safe interaction with client methods and widgets. 

- Queues the RESUME_COUNTDIALOG packet using ctx.widgetPackets.queueResumeCount.
- Checks WidgetInfo.CHATBOX_INPUT and WidgetInfo.CHATBOX_FULL_INPUT for presence.
- Executes a client script (138) if either widget is detected.

#### Parameters

Kraken API

| | |
|---|---|
| value | The numeric input to resume the dialogue with. This value is typically entered by the player in a dialogue box and represents the amount or quantity the player has specified. |
