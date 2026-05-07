//[kraken-api](../../../index.md)/[com.kraken.api.service.magic.rune](../index.md)/[ElementalTome](index.md)/[getRunes](get-runes.md)

# getRunes

[Kraken API]\
open fun [getRunes](get-runes.md)(): [Map](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/Map.html)&lt;[Rune](../-rune/index.md), [Integer](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Integer.html)&gt;

Retrieves a mapping of runes currently provided by the equipped elemental tome. 

 This method checks the player's equipment slot for an equipped elemental tome (e.g., Tome of Fire, Tome of Water, etc.). If an elemental tome is found in the shield slot, it retrieves the associated rune and maps it with an effectively infinite supply (represented by [MAX_VALUE](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Integer.html#MAX_VALUE--)). 

 If no elemental tome is equipped or the equipped item does not correspond to a recognized tome, an empty map is returned. 

**Note:** This method currently only supports single-element elemental tomes. Future expansions may include support for combination runes provided by tomes.

#### Return

A `Map<Rune, Integer>` where the key represents the `Rune` and the value represents the quantity (always [MAX_VALUE](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Integer.html#MAX_VALUE--) for valid matches). Returns an empty map if no matching elemental tome is equipped or recognized.
