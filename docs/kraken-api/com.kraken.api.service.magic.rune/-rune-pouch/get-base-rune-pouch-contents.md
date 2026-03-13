//[kraken-api](../../../index.md)/[com.kraken.api.service.magic.rune](../index.md)/[RunePouch](index.md)/[getBaseRunePouchContents](get-base-rune-pouch-contents.md)

# getBaseRunePouchContents

[Kraken API]\
open fun [getBaseRunePouchContents](get-base-rune-pouch-contents.md)(): [Map](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/Map.html)&lt;[Rune](../-rune/index.md), [Integer](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Integer.html)&gt;

Retrieves the base contents of the rune pouch, converting any combination runes into their respective basic elemental runes. 

This method first obtains the current contents of the rune pouch through [getRunePouchContents](get-rune-pouch-contents.md). If any combination runes are present in the pouch, they are decomposed into their corresponding base runes (e.g., a Dust Rune is split into both Air and Earth Runes). The output is a flattened mapping containing all base runes and their counts, ensuring that combination runes are fully represented by their components.

Note: The quantity of each base rune will match the total quantity of the combination rune that produces it.

- If the rune pouch contains only non-combination runes, the result will be identical to the original mapping from [getRunePouchContents](get-rune-pouch-contents.md).
- If the rune pouch contains a combination rune, all its base components will be added to the returned map with identical counts.
- If the rune pouch is empty or the player does not have a rune pouch, an empty map is returned.

#### Return

A `Map<Rune, Integer>` where: 

- The key is a [Rune](../-rune/index.md) instance representing a base rune included in the pouch contents.
- The value is an `Integer` representing the quantity of that rune.

Combination runes are decomposed into their base components, and the returned map includes the quantities of these base components.
