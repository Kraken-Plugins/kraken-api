package com.kraken.api.core.packet.model;

import com.kraken.api.core.packet.PacketBufferReader;
import lombok.Data;

@Data
public class PacketData {
    private final byte[] data;
    private final int length;
    private final long timestamp;

    public String toHexString() {
        return PacketBufferReader.toHexString(data);
    }
}