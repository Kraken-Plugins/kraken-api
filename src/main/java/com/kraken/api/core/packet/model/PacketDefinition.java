package com.kraken.api.core.packet.model;

import lombok.*;

@Getter
@ToString
@NoArgsConstructor
@EqualsAndHashCode
@AllArgsConstructor
public class PacketDefinition {
    private String packetName;
    private String obfuscatedName;
    private PacketWrite[] writes;

    /**
     * The packet type enum derived from the packet name
     * @return PacketType
     */
    public PacketType getType() {
        if(packetName == null) {
            throw new IllegalStateException("Unknown packet, packetName field is null.");
        }

        return PacketType.valueOf(packetName);
    }
}