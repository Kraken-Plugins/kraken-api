//[lib](../../../index.md)/[com.kraken.api.service.pathfinding](../index.md)/[SparseBitSet](index.md)/[hashCode](hash-code.md)

# hashCode

[Kraken API]\
open fun [hashCode](hash-code.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)

Returns a hash code value for this bit set. The hash code depends only on which bits have been set within this `SparseBitSet`. The algorithm used to compute it may be described as follows. 

 Suppose the bits in the `SparseBitSet` were to be stored in an array of `long` integers called, say, `bits`, in such a manner that bit `i` is set in the `SparseBitSet` (for nonnegative values of `i`) if and only if the expression 

```kotlin
 ((i>>6) < bits.length) && ((bits[i>>6] & (1L << (bit & 0x3F))) != 0)

```
 is true. Then the following definition of the `hashCode` method would be a correct implementation of the actual algorithm: ```kotlin
 public int hashCode()
 {
     long hash = 1234L;
     for( int i = bits.length; --i >= 0; )
         hash ^= bits[i] * (i + 1);
     return (int)((h >> 32) ^ h);
 }
```
 Note that the hash code values change if the set of bits is altered.

#### Return

a hash code value for this bit set

#### Since

1.6

#### See also

| |
|---|
| [Object](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Object.html#equals-java.lang.Object-) |
| [Hashtable](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/Hashtable.html) |
