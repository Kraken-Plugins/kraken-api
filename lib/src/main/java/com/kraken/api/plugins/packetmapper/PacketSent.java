package com.kraken.api.plugins.packetmapper;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PacketSent {
    Object packetBuffer;
}
