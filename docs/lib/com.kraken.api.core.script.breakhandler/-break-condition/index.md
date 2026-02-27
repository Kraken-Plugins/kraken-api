//[lib](../../../index.md)/[com.kraken.api.core.script.breakhandler](../index.md)/[BreakCondition](index.md)

# BreakCondition

[Kraken API]\
@[FunctionalInterface](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/FunctionalInterface.html)

interface [BreakCondition](index.md)

## Functions

| Name | Summary |
|---|---|
| [getDescription](get-description.md) | [Kraken API]<br>open fun [getDescription](get-description.md)(): [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)<br>Optional description of the condition for logging purposes. |
| [shouldBreak](should-break.md) | [Kraken API]<br>abstract fun [shouldBreak](should-break.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Determines if a break should be triggered. |
