//[kraken-api](../../../../index.md)/[com.kraken.api.core.packet.v2](../../index.md)/[SearchStrategy](../index.md)/[BY_DESCRIPTOR_TYPE](index.md)

# BY_DESCRIPTOR_TYPE

[Kraken API]\
[BY_DESCRIPTOR_TYPE](index.md)

Match fields whose descriptor references a class by its non-obfuscated name. searchName should be the non-obfuscated class name (e.g. &quot;IsaacCipher&quot;). The resolver will look up that class's obfuscated name at runtime, then scan for fields with descriptor &quot;L{obfuscatedName};&quot;. This avoids any hardcoded obfuscated values.
