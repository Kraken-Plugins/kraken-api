//[lib](../../../index.md)/[com.kraken.api.service.pathfinding](../index.md)/[SparseBitSet](index.md)/[nextSetBit](next-set-bit.md)

# nextSetBit

[Kraken API]\
open fun [nextSetBit](next-set-bit.md)(i: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)

Returns the index of the first bit that is set to `true` that occurs on or after the specified starting index. If no such it exists then -1 is returned. 

 To iterate over the `true` bits in a `SparseBitSet 
     sbs`, use the following loop: 

```kotlin
 for( int i = sbbits.nextSetBit(0); i >= 0; i = sbbits.nextSetBit(i+1) )
 {
     // operate on index i here
 }
```

#### Return

the index of the next set bit

#### Since

1.6

#### Parameters

Kraken API

| | |
|---|---|
| i | the index to start checking from (inclusive) |

#### Throws

| | |
|---|---|
| [IndexOutOfBoundsException](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/IndexOutOfBoundsException.html) | if the specified index is negative |
