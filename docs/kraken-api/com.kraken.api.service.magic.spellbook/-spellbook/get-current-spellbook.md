//[kraken-api](../../../index.md)/[com.kraken.api.service.magic.spellbook](../index.md)/[Spellbook](index.md)/[getCurrentSpellbook](get-current-spellbook.md)

# getCurrentSpellbook

[Kraken API]\
open fun [getCurrentSpellbook](get-current-spellbook.md)(): [Spellbook](index.md)

Retrieves the current spellbook being used in the game. 

 The method determines the active spellbook by checking the corresponding game variable (varbit) value and matching it with the `value` of the enum constants defined in `Spellbook`. 

 If no match is found, the `STANDARD` spellbook is returned by default. 

#### Return

The active [Spellbook](index.md) instance, which represents the currently selected spellbook in the game. Defaults to `STANDARD` if no match is found.
