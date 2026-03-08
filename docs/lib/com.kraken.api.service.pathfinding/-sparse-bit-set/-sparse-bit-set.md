//[lib](../../../index.md)/[com.kraken.api.service.pathfinding](../index.md)/[SparseBitSet](index.md)/[SparseBitSet](-sparse-bit-set.md)

# SparseBitSet

[Kraken API]\
constructor()

Constructs an empty bit set with the default initial size. Initially all bits are effectively `false`.

#### Since

1.6

[Kraken API]\
constructor(nbits: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html))

Creates a bit set whose initial size is large enough to efficiently represent bits with indices in the range `0` through at least `nbits-1`. Initially all bits are effectively `false`. 

 No guarantees are given for how large or small the actual object will be. The setting of bits above the given range is permitted (and will perhaps eventually cause resizing).

#### Since

1.6

#### Parameters

Kraken API

| | |
|---|---|
| nbits | the initial provisional length of the SparseBitSet |

#### See also

| |
|---|
| [SparseBitSet()](-sparse-bit-set.md) |

#### Throws

| | |
|---|---|
| [NegativeArraySizeException](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/NegativeArraySizeException.html) | if the specified initial length is negative |
