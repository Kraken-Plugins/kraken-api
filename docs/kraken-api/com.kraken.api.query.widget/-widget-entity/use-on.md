//[kraken-api](../../../index.md)/[com.kraken.api.query.widget](../index.md)/[WidgetEntity](index.md)/[useOn](use-on.md)

# useOn

[Kraken API]\
open fun [useOn](use-on.md)(destinationWidget: Widget): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Uses a widget on another widget. (i.e. High Alchemy)

#### Return

True if the action is successful and false otherwise.

#### Parameters

Kraken API

| | |
|---|---|
| destinationWidget | The destination widget to use this entity on |

[Kraken API]\
open fun [useOn](use-on.md)(npc: NPC): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Uses a widget on an NPC (i.e. Crumble Undead Spell on the Undead Spawn from Vorkath)

#### Return

True if the action was successful and false otherwise.

#### Parameters

Kraken API

| | |
|---|---|
| npc | NPC to use the widget on. |

[Kraken API]\
open fun [useOn](use-on.md)(gameObject: GameObject): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Uses a widget on a Game Object (i.e. Bones on the Chaos Altar)

#### Return

True if the action was successful and false otherwise.

#### Parameters

Kraken API

| | |
|---|---|
| gameObject | The Game Object to use the widget on. |
