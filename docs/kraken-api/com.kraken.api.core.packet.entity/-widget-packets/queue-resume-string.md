//[kraken-api](../../../index.md)/[com.kraken.api.core.packet.entity](../index.md)/[WidgetPackets](index.md)/[queueResumeString](queue-resume-string.md)

# queueResumeString

[Kraken API]\
open fun [queueResumeString](queue-resume-string.md)(string: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html))

Queues the RESUME_STRINGDIALOG packet, sent in response to a chat dialog asking the player to enter a generic string (e.g., a search query). 

 Note: The packet data includes the length of the string plus one for the null terminator.

#### Parameters

Kraken API

| | |
|---|---|
| string | The string input entered by the player. |
