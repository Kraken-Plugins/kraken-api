package com.kraken.api.core.packet.model;

import lombok.*;

/**
 * One parameter of a packet definition: the parameter it encodes, the obfuscated client buffer method it was
 * mapped to, and the operations applied to the value before it is written.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PacketWrite {
    /** The name of the packet parameter this write encodes. */
    private String param;
    /** The obfuscated client buffer method this write was mapped to. */
    private String methodName;
    /** The operations applied in order to encode the parameter's value. */
    private BufferOperation[] operations;
}
