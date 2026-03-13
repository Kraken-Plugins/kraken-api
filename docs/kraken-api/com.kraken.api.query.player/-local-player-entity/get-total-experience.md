//[kraken-api](../../../index.md)/[com.kraken.api.query.player](../index.md)/[LocalPlayerEntity](index.md)/[getTotalExperience](get-total-experience.md)

# getTotalExperience

[Kraken API]\
open fun [getTotalExperience](get-total-experience.md)(): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)

Calculates the total experience across all skills for the current client. 

This method iterates through all the skill categories, retrieves their individual experience values, and then sums them up to return the total accumulated experience.

#### Return

the total experience points of all skills combined for the client.
