//[kraken-api](../../../index.md)/[com.kraken.api.service.dialogue](../index.md)/[DialogueService](index.md)/[getDialogueOptions](get-dialogue-options.md)

# getDialogueOptions

[Kraken API]\
open fun [getDialogueOptions](get-dialogue-options.md)(): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;[String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)&gt;

Retrieves a list of dialogue options currently available in the dialogue interface. 

 This method interacts with the client thread to extract options from the appropriate dialogue widget. It filters and returns only the non-blank options. 

 The options are extracted from a pre-defined widget group and are returned as a list of strings. If no options are available or if there is an issue accessing the widget, an empty list is returned. 

#### Return

A [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html) of [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html) containing the text of available dialogue options. If no options are available, the list will be empty.
