package com.kraken.api.core.interceptor.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PacketSent {
    EncodedPacket packet; // The encoded packet data including id, size, and payload byte array
}
