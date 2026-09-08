package com.kraken.api.core.packet.model;

import lombok.*;

@Getter
@ToString
@NoArgsConstructor
@EqualsAndHashCode
@AllArgsConstructor
public class PacketDefinition {
    private String name;
    private String obfuscatedName;
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