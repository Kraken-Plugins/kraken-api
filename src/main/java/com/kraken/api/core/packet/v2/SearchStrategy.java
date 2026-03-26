package com.kraken.api.core.packet.v2;

public enum SearchStrategy {
    /**
     * Default. Match entries where the non-obfuscated "name" field equals searchName.
     */
    BY_NAME,

    /**
     * Match fields whose descriptor references a class by its non-obfuscated name.
     * searchName should be the non-obfuscated class name (e.g. "IsaacCipher").
     * The resolver will look up that class's obfuscated name at runtime,
     * then scan for fields with descriptor "L{obfuscatedName};".
     * This avoids any hardcoded obfuscated values.
     */
    BY_DESCRIPTOR_TYPE,

    /**
     * Match fields whose descriptor exactly equals searchName.
     * Useful for unnamed fields with a distinctive primitive or array descriptor.
     * Use ownerName to narrow to a specific class — strongly recommended here
     * since primitive descriptors like "J" will appear across many classes.
     */
    BY_DESCRIPTOR
}