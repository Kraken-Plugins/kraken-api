//[kraken-api](../../../index.md)/[com.kraken.api.service.magic.rune](../index.md)/[ElementalStaff](index.md)/[getRunes](get-runes.md)

# getRunes

[Kraken API]\
open fun [getRunes](get-runes.md)(): [Map](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/Map.html)&lt;[Rune](../-rune/index.md), [Integer](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Integer.html)&gt;

Retrieves a mapping of runes provided by the currently equipped staff and their quantities. 

This method determines the equipped weapon in the @literal WEAPON equipment slot and identifies whether it corresponds to an `ElementalStaff`. If the equipped staff provides runes (either a single rune or base runes for combination runes), it returns a map where the keys are the runes and the values are set to `Integer.MAX_VALUE`, indicating unlimited availability of those runes.

- If no staff is equipped in the weapon slot, the method returns an empty map.
- If the equipped weapon does not correspond to an `ElementalStaff`, the method returns an empty map.
- If the staff provides a single rune, the map contains the rune with unlimited quantity.
- If the staff provides combination runes, the map contains the base runes with unlimited quantity.

#### Return

A `Map<Rune, Integer>` where the keys represent the runes provided by the currently equipped staff and the values represent their quantities (or an empty map if no applicable staff is equipped).
