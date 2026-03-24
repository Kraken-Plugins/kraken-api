//[kraken-api](../../../index.md)/[com.kraken.api.core](../index.md)/[AbstractQuery](index.md)/[distinct](distinct.md)

# distinct

[Kraken API]\
open fun [distinct](distinct.md)(keyExtractor: ([T](index.md)) -&gt; [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)): [Q](index.md)

Filters the stream to only include elements that are distinct based on a property. Usage: `ctx.npcs().distinct(NpcEntity::getName).list();` (Returns one of each type of NPC nearby)

#### Return

Q The distinct entities

#### Parameters

Kraken API

| | |
|---|---|
| keyExtractor | The function to use to determine uniqueness keys between entities |
