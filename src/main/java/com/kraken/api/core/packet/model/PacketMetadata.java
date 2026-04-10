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
public class PacketMetadata {
    private String mouseHookDllClassName = "client";
    private String mouseHookDllMethodName;

    // Arg added to the creation of packets (same for all packets).
    private String isaacCipherFieldName;

    // The method which actually adds the packet to the outgoing queue to send it
    // to OSRS servers
    private String addNodeMethodName;
    private String addNodeClassName;
    private String addNodeGarbageValue;

    // The class containing all the specific packet definitions (these are used when creating a PacketBufferNode obj)
    private String clientPacketClassName;

    // The class and method which handles writing the packets
    private String packetWriterClassName;
    private String packetWriterFieldName;

    private String classContainingPacketBufferNodeName;
    private String packetBufferNodeFactoryMethodName;

    // Handles writing into the PacketBufferNode object (add 128, >> 8, sub 128 methods etc...)
    private String bufferClassName; // Class implementing RuneLites PacketBuffer
    private String extendedBufferClassName; // bufferClassName extends this class (which is where the actual write methods are)
    private String offsetMultiplier; // Used by the extendedBufferClassName as an obfuscation value to write into the buffer (needed for reflection)
    private String indexMultiplier; // Same as above ^
    private String bufferOffsetField;
    private String bufferArrayField;

    private String mouseHandlerLastPressedClass;
    private String mouseHandlerLastPressedField;
    private String mouseHandlerMultiplier;
    private String clientMillisField;
    private String clientMillisMultiplier; // TODO Find which method this exists in and fingerprint it

    // The class and method that contains a generic packet buffer (an object which can encapsulate
    // any packet data)
    private String packetBufferNodeClassName;
    private String packetBufferFieldName;
    private String packetBufferNodeGarbageValue;

    // Handle Menu action clicks (game objects, NPCs, ground items, etc...) a lot of
    // packets are mapped via this key class/method
    private String doActionClassName;
    private String doActionMethodName;
    private String doActionGarbageValue;

    @Override
    public String toString() {
        return "PacketMetadata {\n" +
                "  mouseHookDllMethodName='" + mouseHookDllMethodName + "',\n" +
                "  isaacCipherFieldName='" + isaacCipherFieldName + "',\n" +
                "  addNodeMethodName='" + addNodeMethodName + "',\n" +
                "  addNodeClassName='" + addNodeClassName + "',\n" +
                "  addNodeGarbageValue='" + addNodeGarbageValue + "',\n" +
                "  clientPacketClassName='" + clientPacketClassName + "',\n" +
                "  packetWriterClassName='" + packetWriterClassName + "',\n" +
                "  packetWriterFieldName='" + packetWriterFieldName + "',\n" +
                "  classContainingPacketBufferNodeName='" + classContainingPacketBufferNodeName + "',\n" +
                "  packetBufferNodeFactoryMethodName='" + packetBufferNodeFactoryMethodName + "',\n" +
                "  bufferClassName='" + bufferClassName + "',\n" +
                "  extendedBufferClassName='" + extendedBufferClassName + "',\n" +
                "  offsetMultiplier='" + offsetMultiplier + "',\n" +
                "  indexMultiplier='" + indexMultiplier + "',\n" +
                "  bufferOffsetField='" + bufferOffsetField + "',\n" +
                "  bufferArrayField='" + bufferArrayField + "',\n" +
                "  packetBufferNodeClassName='" + packetBufferNodeClassName + "',\n" +
                "  packetBufferFieldName='" + packetBufferFieldName + "',\n" +
                "  packetBufferNodeGarbageValue='" + packetBufferNodeGarbageValue + "',\n" +
                "  doActionClassName='" + doActionClassName + "',\n" +
                "  doActionMethodName='" + doActionMethodName + "'\n" +
                "  doActionGarbageValue='" + doActionGarbageValue + "'\n" +
                "  mouseHandlerLastPressedClass='" + mouseHandlerLastPressedClass + "'\n" +
                "  mouseHandlerLastPressedField='" + mouseHandlerLastPressedField + "'\n" +
                "  mouseHandlerMultiplier='" + mouseHandlerMultiplier + "'\n" +
                "  clientMillisMultiplier='" + clientMillisMultiplier + "'\n" +
                "  clientMillisField='" + clientMillisField + "'\n" +
                "}";
    }
}
