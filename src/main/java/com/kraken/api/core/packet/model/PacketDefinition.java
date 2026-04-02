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
    private PacketType type;
}