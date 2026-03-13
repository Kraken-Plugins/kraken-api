//[kraken-api](../../../index.md)/[com.kraken.api.service.pathfinding](../index.md)/[SparseBitSet](index.md)/[equals](equals.md)

# equals

[Kraken API]\
open fun [equals](equals.md)(obj: [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Compares this object against the specified object. The result is `true` if and only if the argument is not `null` and is a `SparseBitSet` object that has exactly the same bits set to `true` as this bit set. That is, for every nonnegative `i` indexing a bit in the set, 

```kotlin
((SparseBitSet)obj).get(i) == this.get(i)
```
 must be true.

#### Return

`true` if the objects are equivalent; `false` otherwise.

#### Since

1.6

#### Parameters

Kraken API

| | |
|---|---|
| obj | the Object with which to compare |
