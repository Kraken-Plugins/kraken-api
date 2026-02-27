//[lib](../../../index.md)/[com.kraken.api.service.ui.dialogue](../index.md)/[DialogueService](index.md)/[getDialogueHeader](get-dialogue-header.md)

# getDialogueHeader

[Kraken API]\
open fun [getDialogueHeader](get-dialogue-header.md)(): [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)

Retrieves the dialogue header text currently displayed in the widget interface. This method determines the source of the dialogue and provides an appropriate header. 

 The header may represent: 

- The NPC's name if an NPC dialogue is active.
- &quot;Player&quot; if a player dialogue is active.
- &quot;Select an Option&quot; if a dialogue option selection is active.
- &quot;unknown&quot; if no known dialogue state is detected.

#### Return

The dialogue header text as a @String, which identifies the current dialogue source.
