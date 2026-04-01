package com.kraken.api.core.mapping;

import com.kraken.api.core.packet.v2.MappingsResolver;
import com.kraken.api.core.packet.v2.ObfuscatedMapping;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@Slf4j
public class ClientUpdater {

    private static final String CLIENT_VERSION = "1.12.23";

    public static void main(String[] args) throws IOException {
        Path input = Paths.get(System.getProperty("user.home"), "injected-client-" + CLIENT_VERSION + ".jar");
        Path output = Paths.get(System.getProperty("user.home"), "injected-client-deob-" + CLIENT_VERSION + ".jar");

        // 1. Download the obfuscated injected client
        log.info("Downloading injected client version {}", CLIENT_VERSION);
        Path client = ClientDownloader.downloadInjectedClient(CLIENT_VERSION, input);
        if (client == null) {
            log.error("Failed to download client jar");
            return;
        }

        // 2. Resolve hardcoded mappings using your existing resolver
        log.info("Resolving static client mappings...");
        MappingsResolver resolver = new MappingsResolver();
        Map<ObfuscatedMapping, Object> mappings = resolver.resolveAll();

        String packetWriterField = (String) mappings.get(ObfuscatedMapping.PACKET_WRITER_FIELD_NAME);
        String packetWriterClass = (String) mappings.get(ObfuscatedMapping.PACKET_WRITER_CLASS_NAME);
        int   indexMultiplier   = (Integer) mappings.get(ObfuscatedMapping.INDEX_MULTIPLIER);
        int    addNodeGarbage    = (Integer) mappings.get(ObfuscatedMapping.ADD_NODE_GARBAGE_VALUE);

        // 3. Deobfuscate the client (strips annotations, cleans doAction)
        try {
            Deobfuscator.run(input, output);
        } catch (Exception e) {
            log.error("Failed to deobfuscate client", e);
            return;
        }

        // 4. Run the Packet Mapper on the newly cleaned JAR
        log.info("Starting PacketMapper on deobfuscated client...");
        try {
            new PacketMapper().run(output); // Run on the OUTPUT (deobbed) jar, not the input!
        } catch (Exception e) {
            log.error("Failed during packet mapping execution", e);
        }
    }
}