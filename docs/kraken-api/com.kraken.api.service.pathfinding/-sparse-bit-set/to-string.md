//[kraken-api](../../../index.md)/[com.kraken.api.service.pathfinding](../index.md)/[SparseBitSet](index.md)/[toString](to-string.md)

# toString

[Kraken API]\
open fun [toString](to-string.md)(): [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)

Returns a string representation of this bit set. For every index for which this `SparseBitSet` contains a bit in the set state, the decimal representation of that index is included in the result. Such indices are listed in order from lowest to highest. If there is a subsequence of set bits longer than the value given by toStringCompaction, the subsequence is represented by the value for the first and the last values, with &quot;..&quot; between them. The individual bits, or the representation of sub-sequences are separated by &quot;, &quot; (a comma and a space) and surrounded by braces, resulting in a compact string showing (a variant of) the usual mathematical notation for a set of integers.  Example (with the default value of 2 for subsequences): 

```kotlin
     SparseBitSet drPepper = new SparseBitSet();

```
 Now `drPepper.toString()` returns &quot;`{}`&quot;. ```kotlin
     drPepper.set(2);

```
 Now `drPepper.toString()` returns &quot;`{2}`&quot;. ```kotlin
     drPepper.set(3, 4);
     drPepper.set(10);

```
 Now `drPepper.toString()` returns &quot;`{2..4, 10}`&quot;.  This method is intended for diagnostic use (as it is relatively expensive in time), but can be useful in interpreting problems in an application's use of a `SparseBitSet`.

#### Return

a String representation of this SparseBitSet

#### Since

1.6

#### See also

| |
|---|
| [toStringCompaction(int length)](to-string-compaction.md) |
