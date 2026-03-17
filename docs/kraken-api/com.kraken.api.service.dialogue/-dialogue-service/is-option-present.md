//[kraken-api](../../../index.md)/[com.kraken.api.service.dialogue](../index.md)/[DialogueService](index.md)/[isOptionPresent](is-option-present.md)

# isOptionPresent

[Kraken API]\
open fun [isOptionPresent](is-option-present.md)(option: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Checks if a specific dialogue option is present in the list of available options. 

 The search is case-insensitive and supports partial matches. If the provided option text matches any part of any dialogue option, the method will return true. 

#### Return

`true` if the option is found in the list of available dialogue options, `false` otherwise.

#### Parameters

Kraken API

| | |
|---|---|
| option | The text of the dialogue option to search for. This parameter is case-insensitive and partial matches are supported. |
