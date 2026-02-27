//[lib](../../../index.md)/[com.kraken.api.service.ui.dialogue](../index.md)/[DialogueService](index.md)/[continueObjectDialogue](continue-object-dialogue.md)

# continueObjectDialogue

[Kraken API]\
open fun [continueObjectDialogue](continue-object-dialogue.md)(id: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html))

Resumes an object-based dialogue in the game by interacting with relevant widgets and invoking a client-side script to continue the process. 

 This method is specifically designed to handle dialogues involving object IDs and ensures that the appropriate actions are executed to progress through the dialogue. It queues a RESUME_OBJDIALOG packet with the given object ID and checks for specific chatbox input widgets to determine whether a client script needs to be executed. If either of the relevant widgets is detected, a predefined client script is invoked. 

 The execution occurs on the client thread to ensure proper synchronization with the game's UI and safe interaction with client methods and widgets. 

- Queues the RESUME_OBJDIALOG packet using ctx.widgetPackets.queueResumeObj.
- Checks WidgetInfo.CHATBOX_INPUT and WidgetInfo.CHATBOX_FULL_INPUT for presence.
- Executes a client script (138) if either widget is detected.

#### Parameters

Kraken API

| | |
|---|---|
| id | The ID of the object used in the dialogue. This ID represents the object or item referenced by the dialogue option and ensures its selection and continuation. |
