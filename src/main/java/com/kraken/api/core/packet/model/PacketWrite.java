package com.kraken.api.core.packet.model;

import lombok.*;

@Getter
@ToString
@NoArgsConstructor
@EqualsAndHashCode
@AllArgsConstructor
public class PacketWrite {
    private String param;
    private String methodName;
    private BufferOperation[] operations;
}
