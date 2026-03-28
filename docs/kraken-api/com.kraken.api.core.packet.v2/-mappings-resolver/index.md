//[kraken-api](../../../index.md)/[com.kraken.api.core.packet.v2](../index.md)/[MappingsResolver](index.md)

# MappingsResolver

[Kraken API]\
open class [MappingsResolver](index.md)

Parses mappings.json and resolves all ObfuscatedMapping entries. Zero obfuscated values are used during lookup — only non-obfuscated names.

## Constructors

| | |
|---|---|
| [MappingsResolver](-mappings-resolver.md) | [Kraken API]<br>constructor() |

## Functions

| Name | Summary |
|---|---|
| [loadMappings](load-mappings.md) | [Kraken API]<br>open fun [loadMappings](load-mappings.md)()<br>Fetches and parses mappings.json from MinIO. |
| [resolve](resolve.md) | [Kraken API]<br>open fun [resolve](resolve.md)(mapping: [ObfuscatedMapping](../-obfuscated-mapping/index.md)): [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html) |
| [resolveAll](resolve-all.md) | [Kraken API]<br>open fun [resolveAll](resolve-all.md)(): [Map](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/Map.html)&lt;[ObfuscatedMapping](../-obfuscated-mapping/index.md), [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-any/index.html)&gt;<br>Resolves all mappings at once. |
