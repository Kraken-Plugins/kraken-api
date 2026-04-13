package com.kraken.api.core.packet.model;

import lombok.Data;

import javax.inject.Singleton;

/**
 * Additional metadata extracted from the fingerprint classes when
 * scanning the client classes and methods. These values are used throughout
 * the mapping process and are used in the packet sending process via reflection.
 */
@Data
@Singleton
public final class PacketMetadata {

    private final String mouseHookDllClassName = "client";
    private final String mouseHookDllMethodName;

    private final String isaacCipherFieldName;

    private final String addNodeMethodName;
    private final String addNodeClassName;
    private final Integer addNodeGarbageValue;

    private final String clientPacketClassName;

    private final String packetWriterClassName;
    private final String packetWriterFieldName;

    private final String classContainingPacketBufferNodeName;
    private final String packetBufferNodeFactoryMethodName;

    private final String bufferClassName;
    private final String extendedBufferClassName;
    private final Integer offsetMultiplier;
    private final Integer indexMultiplier;
    private final String bufferOffsetField;
    private final String bufferArrayField;

    private final String mouseHandlerLastPressedClass;
    private final String mouseHandlerLastPressedField;
    private final Long mouseHandlerMultiplier;
    private final String clientMillisField;
    private final Long clientMillisMultiplier;

    private final String packetBufferNodeClassName;
    private final String packetBufferFieldName;
    private final Integer packetBufferNodeGarbageValue;

    private final String doActionClassName;
    private final String doActionMethodName;
    private final Integer doActionGarbageValue;
}
