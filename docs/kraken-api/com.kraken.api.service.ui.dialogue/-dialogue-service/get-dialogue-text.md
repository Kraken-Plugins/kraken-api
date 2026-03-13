//[kraken-api](../../../index.md)/[com.kraken.api.service.ui.dialogue](../index.md)/[DialogueService](index.md)/[getDialogueText](get-dialogue-text.md)

# getDialogueText

[Kraken API]\
open fun [getDialogueText](get-dialogue-text.md)(): [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)

Retrieves the text from the currently active dialogue widget if available. This method checks multiple possible widgets for dialogue text, including NPC text, player text, sprite-based text, and other specific dialogue-related interfaces. 

The method will return the first non-null and non-empty text identified from the following widgets: 

- WidgetInfo.DIALOG_NPC_TEXT
- WidgetInfo.DIALOG_PLAYER_TEXT
- WidgetInfo.DIALOG_SPRITE_TEXT
- ctx.widgets().get(11, 2)
- ctx.widgets().get(229, MinigameDialog.TEXT)
- ctx.widgets().get(229, DialogNotification.TEXT)
- ctx.widgets().get(InterfaceID.Messagebox.TEXT)

If no dialogue text is found across any of these widgets, an empty string will be returned.

#### Return

A String containing the dialogue text from the active widget, or an empty string if no text is available.
