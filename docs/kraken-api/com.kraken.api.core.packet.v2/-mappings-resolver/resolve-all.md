//[kraken-api](../../../index.md)/[com.kraken.api.core.packet.v2](../index.md)/[MappingsResolver](index.md)/[resolveAll](resolve-all.md)

# resolveAll

[Kraken API]\
open fun [resolveAll](resolve-all.md)(): [Map](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/Map.html)&lt;[ObfuscatedMapping](../-obfuscated-mapping/index.md), [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)&gt;

Resolves all mappings at once. Logs a warning for any that can't be found rather than blowing up, so you can see all failures at once on startup.

#### Return

Map of ObfuscatedMapping to resolved value.
