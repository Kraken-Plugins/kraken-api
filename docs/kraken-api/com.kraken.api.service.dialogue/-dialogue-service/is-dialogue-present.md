//[kraken-api](../../../index.md)/[com.kraken.api.service.dialogue](../index.md)/[DialogueService](index.md)/[isDialoguePresent](is-dialogue-present.md)

# isDialoguePresent

[Kraken API]\
open fun [isDialoguePresent](is-dialogue-present.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Checks if any type of dialogue is currently present in the game client. 

 This method scans various widget IDs associated with different dialogue types such as NPC dialogue, player dialogue, level-up messages, notifications, and more, to determine if a relevant dialogue is displayed. 

- Includes checks for NPC and player dialogues.
- Handles level-up and notification dialogues.
- Supports minigame-related and chatbox dialogues.
- Accounts for &quot;Click here to continue&quot; prompts.
- Detects option dialogues for user decisions.

#### Return

true if any dialogue or clickable interaction is active, false otherwise.
