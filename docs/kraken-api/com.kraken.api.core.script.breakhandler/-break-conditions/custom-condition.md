//[kraken-api](../../../index.md)/[com.kraken.api.core.script.breakhandler](../index.md)/[BreakConditions](index.md)/[customCondition](custom-condition.md)

# customCondition

[Kraken API]\
open fun [customCondition](custom-condition.md)(shouldBreakCheck: [Supplier](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/function/Supplier.html)&lt;[Boolean](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Boolean.html)&gt;, reason: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)): [BreakCondition](../-break-condition/index.md)

A custom break condition which can include any logic

#### Return

BreakCondition

#### Parameters

Kraken API

| | |
|---|---|
| shouldBreakCheck | Supplier which returns a boolean. When true a break will occur. |
| reason | The plain text reason for the break |
