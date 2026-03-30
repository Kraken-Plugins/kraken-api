package com.kraken.api.core.mapping;

import com.kraken.api.core.packet.v2.MappingsResolver;
import com.kraken.api.core.packet.v2.ObfuscatedMapping;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@Slf4j
public class ClientMapper {

    private static final Path RUNELITE_WRITE_PATH = Paths.get(System.getProperty("user.home"), ".runelite", "kraken", "injected-client-1.12.22.1.jar");

    public static void main(String[] args) throws IOException {
        Path input = Paths.get(System.getProperty("user.home"), "injected-client-1.12.22.1.jar");
        Path output = Paths.get(System.getProperty("user.home"), "injected-client-sanitized-1.12.22.1.jar");

        Path client = ClientDownloader.downloadInjectedClient("1.12.22.1", input);
        if(client == null) {
            log.error("Failed to download client jar");
            return;
        }
        AnnotationRemover.stripNamedAnnotations(client, output);

        MappingsResolver resolver = new MappingsResolver();
        Map<ObfuscatedMapping, Object> mappings = resolver.resolveAll();

        String packetWriterField = (String) mappings.get(ObfuscatedMapping.PACKET_WRITER_FIELD_NAME);
        String packetWriterClass = (String) mappings.get(ObfuscatedMapping.PACKET_WRITER_CLASS_NAME);
        long   indexMultiplier   = (Integer)   mappings.get(ObfuscatedMapping.INDEX_MULTIPLIER);
        int    addNodeGarbage    = (Integer) mappings.get(ObfuscatedMapping.ADD_NODE_GARBAGE_VALUE);
    }
}
