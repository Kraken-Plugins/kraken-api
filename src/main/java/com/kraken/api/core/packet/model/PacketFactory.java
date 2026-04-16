package com.kraken.api.core.packet.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import com.kraken.api.util.JsonResourceUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>A factory class for creating and managing {@link PacketDefinition} instances for various packet types and actions.
 * This class initializes packet definitions by fetching and parsing a JSON configuration file locally first,
 * then falling back to a remote URL.</p>
 */
@Slf4j
public class PacketFactory {

    @Getter
    private static Map<String, PacketDefinition> packets = new HashMap<>();

    @Getter
    private static PacketMetadata packetMetadata = null;

    @Getter
    private static String clientVersion = "";

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final String LOCAL_PACKETS_PATH = "/packets.json";

    /**
     * Initializes the packet factory by loading packet definitions from local resources or a remote source.
     * <p>
     * This method attempts to load a JSON file containing packet definitions from a predefined
     * local path. If the local file is unavailable or an exception occurs while processing it,
     * the method falls back to retrieving the packet definitions from a remote URL.
     * </p>
     */
    public static void init() {
        try {
            MappedPackets mappedPackets = JsonResourceUtils.loadJsonResource(
                    PacketFactory.class,
                    LOCAL_PACKETS_PATH,
                    gson,
                    MappedPackets.class
            );
            if (mappedPackets.getPackets() == null) {
                throw new IllegalStateException("Parsed MappedPackets or its packet map was null.");
            }

            packets = mappedPackets.getPackets();
            packetMetadata = mappedPackets.getReflectionHooks();
            clientVersion = mappedPackets.getClientVersion();
            log.info("Loaded packets.json from local resources.");
        } catch (Exception e) {
            log.error("Exception while trying to load packets.json.", e);
        }
    }

    /**
     * Helper method to safely retrieve a packet and fail fast if the JSON is missing definitions.
     */
    private static PacketDefinition getPacket(String packetName) {
        PacketDefinition def = packets.get(packetName);
        if (def == null) {
            throw new IllegalStateException("Packet definition not found in mapped JSON for: " + packetName);
        }
        return def;
    }

    // Resume packets for dialogues with NPC's in the chatbox.
    // pause is for basic "CLick here to contine", count is when entering a quantity like: withdraw-X, obj is when
    // picking an object i.e. sailing mermaids or picking an item from a GE search.
    public static PacketDefinition getResumeCountDialog() { return getPacket("RESUME_COUNTDIALOG"); }
    public static PacketDefinition getResumeObjDialog() { return getPacket("RESUME_OBJDIALOG"); }
    public static PacketDefinition getStringDialog() { return getPacket("RESUME_STRINGDIALOG"); }

    // Movement and event packets
    public static PacketDefinition getMoveGameClick() { return getPacket("MOVE_GAMECLICK"); }
    public static PacketDefinition getEventMouseClick() { return getPacket("EVENT_MOUSE_CLICK"); }
}
