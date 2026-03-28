//[kraken-api](../../../index.md)/[com.kraken.api.core.packet.v2](../index.md)/[MappingsResolver](index.md)/[loadMappings](load-mappings.md)

# loadMappings

[Kraken API]\
open fun [loadMappings](load-mappings.md)()

Fetches and parses mappings.json from MinIO. Synchronized to prevent multiple threads from fetching the 2MB file simultaneously on startup.
