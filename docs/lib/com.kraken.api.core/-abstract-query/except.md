//[lib](../../../index.md)/[com.kraken.api.core](../index.md)/[AbstractQuery](index.md)/[except](except.md)

# except

[Kraken API]\
open fun [except](except.md)(predicate: [Predicate](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/function/Predicate.html)&lt;[T](index.md)&gt;): [Q](index.md)

Filters out elements that match the given predicate. Effectively: filter(!predicate)

#### Return

Q All entities except for the ones that match the given predicate

#### Parameters

Kraken API

| | |
|---|---|
| predicate | The predicate to apply |
