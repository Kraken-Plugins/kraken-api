package com.kraken.api.core.packet.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PacketSent {
    Object packetBuffer; // The OSRS client object encapsulating the packet data
    PacketData packet; // The read packet byte array
}
