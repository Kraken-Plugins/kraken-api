package com.kraken.api.core.packet.v2;

import lombok.Getter;

/**
 * Every entry here is located using ONLY non-obfuscated names, so this file
 * survives mappings.json revisions as long as the RuneLite-named fields/classes stay stable.
 *
 * Format: ObfuscatedMapping(searchName, ownerClassName, scope, extract, returnType)
 *   - searchName:     the non-obfuscated "name" field in the JSON to match against
 *   - ownerClassName: the non-obfuscated "name" of the owning class (null = search all classes)
 *   - scope:          FIELD, METHOD, or CLASS
 *   - extract:        which property to pull from the matched entry
 *   - type:           expected Java type of the resolved value
 */
@Getter
public enum ObfuscatedMapping {

    // -------------------------------------------------------------------------
    // Buffer arithmetic multipliers
    // Both come from the same "offset" field in "Buffer", different properties.
    // -------------------------------------------------------------------------

    INDEX_MULTIPLIER("offset", "Buffer", SearchScope.FIELD, ExtractTarget.GETTER, Integer.class),
    OFFSET_MULTIPLIER("offset", "Buffer", SearchScope.FIELD, ExtractTarget.SETTER, Integer.class),
    BUFFER_OFFSET_FIELD("offset", "Buffer", SearchScope.FIELD, ExtractTarget.OBFUSCATED_NAME, String.class),
    BUFFER_ARRAY_FIELD("array", "Buffer", SearchScope.FIELD, ExtractTarget.OBFUSCATED_NAME, String.class),

    // -------------------------------------------------------------------------
    // packetWriter field on Client: get its obfuscated name AND its class name
    // The class name is parsed from the descriptor e.g. "Ldh;" → "dh"
    // -------------------------------------------------------------------------
    PACKET_WRITER_FIELD_NAME("packetWriter", "Client", SearchScope.FIELD, ExtractTarget.OBFUSCATED_NAME, String.class),
    PACKET_WRITER_CLASS_NAME("PacketWriter", null, SearchScope.CLASS, ExtractTarget.OBFUSCATED_NAME, String.class),

    // -------------------------------------------------------------------------
    // addNode method: name + garbage value.
    // -------------------------------------------------------------------------
    ADD_NODE_METHOD_NAME("addNode", "PacketWriter", SearchScope.METHOD, ExtractTarget.OBFUSCATED_NAME, String.class),
    ADD_NODE_GARBAGE_VALUE("addNode", "PacketWriter", SearchScope.METHOD, ExtractTarget.GARBAGE_VALUE, Integer.class),

    // -------------------------------------------------------------------------,
    // ISAAC cipher field on PacketWriter.
    // -------------------------------------------------------------------------

    ISAAC_CIPHER_FIELD_NAME("isaacCipher", "PacketWriter", SearchScope.FIELD, ExtractTarget.OBFUSCATED_NAME, String.class),

    // -------------------------------------------------------------------------
    // ClientPacket class obfuscated name
    // -------------------------------------------------------------------------
    CLIENT_PACKET_CLASS_NAME("ClientPacket", null, SearchScope.CLASS, ExtractTarget.OBFUSCATED_NAME, String.class),

    // -------------------------------------------------------------------------
    // PacketBufferNode class + its packetBuffer field
    // -------------------------------------------------------------------------
    PACKET_BUFFER_NODE_CLASS_NAME("PacketBufferNode", null, SearchScope.CLASS, ExtractTarget.OBFUSCATED_NAME, String.class),
    PACKET_BUFFER_FIELD_NAME("packetBuffer", "PacketBufferNode", SearchScope.FIELD, ExtractTarget.OBFUSCATED_NAME, String.class),
    CLASS_CONTAINING_GET_PACKET_BUFFER_NODE_NAME("getPacketBufferNode", null, SearchScope.METHOD, ExtractTarget.OWNER_OBFUSCATED_NAME, String.class),
    GET_PACKET_BUFFER_NODE_GARBAGE_VALUE("getPacketBufferNode", null, SearchScope.METHOD, ExtractTarget.GARBAGE_VALUE, Integer.class),

    // -------------------------------------------------------------------------
    // MouseHandler: class name, lastPressedTimeMillis field + its multiplier
    // -------------------------------------------------------------------------
    MOUSE_HANDLER_LAST_PRESSED_TIME_MILLIS_CLASS("MouseHandler", null, SearchScope.CLASS, ExtractTarget.OBFUSCATED_NAME, String.class),
    MOUSE_HANDLER_LAST_PRESSED_TIME_MILLIS_FIELD("MouseHandler_lastPressedTimeMillis", "MouseHandler", SearchScope.FIELD, ExtractTarget.OBFUSCATED_NAME, String.class),
    MOUSE_HANDLER_MILLIS_MULTIPLIER("MouseHandler_lastPressedTimeMillis", "MouseHandler", SearchScope.FIELD, ExtractTarget.GETTER, Long.class),


    // -------------------------------------------------------------------------
    // Client last ms fields + multipliers
    // -------------------------------------------------------------------------
    // TODO Difficult to map these two even though they exist on the client class they have no name in mappings.json
    // and only a long descriptor (J = Long.class). If theres another long field in client and it comes first
    // it will map to this possibly incorrectly...
    CLIENT_MILLIS_FIELD(
            "J", "Client", SearchScope.FIELD, SearchStrategy.BY_DESCRIPTOR,
            ExtractTarget.OBFUSCATED_NAME, String.class
    ),
    CLIENT_MILLIS_MULTIPLIER(
            "J", "Client", SearchScope.FIELD, SearchStrategy.BY_DESCRIPTOR,
            ExtractTarget.GETTER, Long.class
    ),


    // -------------------------------------------------------------------------
    // doAction: method name + the class that owns it
    // -------------------------------------------------------------------------
    DO_ACTION_METHOD_NAME(
            "doAction", null, SearchScope.METHOD, ExtractTarget.OBFUSCATED_NAME, String.class
    ),
    DO_ACTION_CLASS_NAME(
            "doAction", null, SearchScope.METHOD, ExtractTarget.OWNER_OBFUSCATED_NAME, String.class
    );

    /** Non-obfuscated name to search for in the JSON */
    private final String searchName;

    /**
     * Non-obfuscated owner class name to narrow the search.
     * Null means "search all classes" — use this only when the method/field
     * name is unique enough across the whole mappings file.
     */
    private final String ownerName;

    private final SearchScope scope;
    private final SearchStrategy strategy;
    private final ExtractTarget extract;
    private final Class<?> type;

    /**
     * Standard constructor — defaults to BY_NAME strategy.
     */
    ObfuscatedMapping(String searchName, String ownerName, SearchScope scope,
                      ExtractTarget extract, Class<?> type) {
        this.searchName = searchName;
        this.ownerName  = ownerName;
        this.scope      = scope;
        this.strategy   = SearchStrategy.BY_NAME;
        this.extract    = extract;
        this.type       = type;
    }

    /**
     * Full constructor — used when a non-default search strategy is needed.
     */
    ObfuscatedMapping(String searchName, String ownerName, SearchScope scope,
                      SearchStrategy strategy, ExtractTarget extract, Class<?> type) {
        this.searchName = searchName;
        this.ownerName  = ownerName;
        this.scope      = scope;
        this.strategy   = strategy;
        this.extract    = extract;
        this.type       = type;
    }
}