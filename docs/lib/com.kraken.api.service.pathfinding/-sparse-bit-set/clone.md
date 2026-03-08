//[lib](../../../index.md)/[com.kraken.api.service.pathfinding](../index.md)/[SparseBitSet](index.md)/[clone](clone.md)

# clone

[Kraken API]\
open fun [clone](clone.md)(): [SparseBitSet](index.md)

Cloning this `SparseBitSet` produces a new `SparseBitSet` that is *equal*() to it. The clone of the bit set is another bit set that has exactly the same bits set to `true` as this bit set. 

 Note: the actual space allocated to the clone tries to minimise the actual amount of storage allocated to hold the bits, while still trying to keep access to the bits being a rapid as possible. Since the space allocated to a `SparseBitSet` is not normally decreased, replacing a bit set by its clone may be a way of both managing memory consumption and improving the rapidity of access.

#### Return

a clone of this SparseBitSet

#### Since

1.6
