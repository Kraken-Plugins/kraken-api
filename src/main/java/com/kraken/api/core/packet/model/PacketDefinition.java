package com.kraken.api.core.packet.model;

import lombok.*;

/**
 * A packet the client sends, as mapped in {@code hooks.json}: its name, its obfuscated name and the writes that
 * encode each of its parameters.
 */
@Getter
@ToString
@NoArgsConstructor
@EqualsAndHashCode
@AllArgsConstructor
public class PacketDefinition {
    /** The packet's name, matching a {@link PacketType} constant. */
    private String name;
    /** The obfuscated name the client uses for this packet. */
    private String obfuscatedName;
    /** The writes that encode each parameter, in the order they are written. */
    private PacketWrite[] writes;

    /**
     * The packet type enum derived from the packet name
     * @return PacketType
     */
    public PacketType getType() {
        if(name == null) {
            throw new IllegalStateException("Unknown packet, name field is null.");
        }

        return PacketType.valueOf(name);
    }
}