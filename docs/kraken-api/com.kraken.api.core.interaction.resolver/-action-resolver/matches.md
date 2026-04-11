//[kraken-api](../../../index.md)/[com.kraken.api.core.interaction.resolver](../index.md)/[ActionResolver](index.md)/[matches](matches.md)

# matches

[Kraken API]\
open fun [matches](matches.md)(requested: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html), candidate: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Compares a requested string with a candidate string to determine if they match as equal. 

 The comparison is case-insensitive and applies sanitation to the candidate string before performing the comparison.

#### Return

`true` if the sanitized `candidate` matches `requested` (case-insensitively); otherwise, `false`.

#### Parameters

Kraken API

| | |
|---|---|
| requested | The string to be matched against. |
| candidate | The string to check, potentially unsanitized. If `candidate` is `null`, the method will return `false`. |
