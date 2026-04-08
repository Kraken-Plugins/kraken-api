package com.kraken.api.core.packet.model;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PacketWrite {
    private String param;
    private String methodName;
    private BufferOperation[] operations;
}
