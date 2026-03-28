package com.kraken.api.core.mapping;

import com.kraken.api.core.packet.v2.MappingsResolver;
import com.kraken.api.core.packet.v2.ObfuscatedMapping;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@Slf4j
public class ClientMapper {

    private static final Path RUNELITE_WRITE_PATH = Paths.get(System.getProperty("user.home"), ".runelite", "kraken", "runelite-1.12.20.jar");

    public static void main(String[] args) {
        ClientDownloader.downloadInjectedClient("1.12.20", RUNELITE_WRITE_PATH);
        MappingsResolver resolver = new MappingsResolver();
        Map<ObfuscatedMapping, Object> mappings = resolver.resolveAll();

        // Type-safe accessors:
        String packetWriterField = (String) mappings.get(ObfuscatedMapping.PACKET_WRITER_FIELD_NAME);
        String packetWriterClass = (String) mappings.get(ObfuscatedMapping.PACKET_WRITER_CLASS_NAME);
        long   indexMultiplier   = (Integer)   mappings.get(ObfuscatedMapping.INDEX_MULTIPLIER);
        int    addNodeGarbage    = (Integer) mappings.get(ObfuscatedMapping.ADD_NODE_GARBAGE_VALUE);

        log.info("Packet writer field: {}", packetWriterField);
        log.info("Packet writer class: {}", packetWriterClass);
        log.info("Index multiplier: {}", indexMultiplier);
        log.info("addNode() garbage: {}", addNodeGarbage);
    }
}
