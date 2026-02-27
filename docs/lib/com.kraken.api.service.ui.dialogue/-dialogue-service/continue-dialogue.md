//[lib](../../../index.md)/[com.kraken.api.service.ui.dialogue](../index.md)/[DialogueService](index.md)/[continueDialogue](continue-dialogue.md)

# continueDialogue

[Kraken API]\
open fun [continueDialogue](continue-dialogue.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Attempts to continue an active dialogue in the game by interacting with various dialogue widgets. 

 This method works by checking for the presence of specific dialogue widgets representing NPC dialogues, player dialogues, notifications, level-up screens, chatbox dialogues, and other message boxes. If any such widget is found and recognized, the method sends a resume or pause command to the appropriate widget to continue the dialogue. The logic prioritizes widgets in a specific order to handle varying dialogue types. 

 The method is executed on the client thread to ensure proper interaction with the game's UI. It returns `true` if a valid dialogue widget was found and the &quot;continue&quot; action was successfully triggered. 

- If an NPC dialogue widget is detected in the `WidgetID.DIALOG_NPC_GROUP_ID` group, it triggers the &quot;continue&quot; action.
- Handles player dialogues, sprite dialogues, level-up dialogues, and other known specific IDs.
- Checks text values, such as &quot;Click here to continue,&quot; in certain widgets to verify the necessity of sending a command.

#### Return

`true` if a valid dialogue widget was interacted with to continue the dialogue, `false` if no applicable dialogue widget was found or interacted with.
