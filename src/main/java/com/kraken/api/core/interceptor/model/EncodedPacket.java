package com.kraken.api.core.interceptor.model;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EncodedPacket {
    private int encodedId;
    private int encodedLength;
    private byte[] payload;
    private Object packetBufferNode;
}