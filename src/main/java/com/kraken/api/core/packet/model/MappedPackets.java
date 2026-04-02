package com.kraken.api.core.packet.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class MappedPackets {
    private String clientVersion;
    private Map<String, PacketDefinition> packets;
}
