//[kraken-api](../../../index.md)/[com.kraken.api.service.magic.rune](../index.md)/[RunePouch](index.md)/[getRunePouchContents](get-rune-pouch-contents.md)

# getRunePouchContents

[Kraken API]\
open fun [getRunePouchContents](get-rune-pouch-contents.md)(): [Map](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/Map.html)&lt;[Rune](../-rune/index.md), [Integer](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Integer.html)&gt;

Retrieves the current contents of the rune pouch if one is present in the player's inventory. 

 The rune pouch can hold up to three types of runes, each represented by a [Rune](../-rune/index.md) object and its corresponding count. This method checks if a rune pouch is available, determines the runes contained within it, and returns a mapping of runes to their quantities. 

- If no rune pouch is found in the player's inventory, an empty map is returned.
- If a rune pouch exists, the method queries the item definitions and in-game variables (varbits) to determine the specific runes and their counts.

#### Return

A `Map<Rune, Integer>` where: 

- The key is a [Rune](../-rune/index.md) instance representing a type of rune stored in the pouch.
- The value is an `Integer` representing the quantity of that rune.

Returns an empty map if no rune pouch is present or if the pouch contains no runes.
