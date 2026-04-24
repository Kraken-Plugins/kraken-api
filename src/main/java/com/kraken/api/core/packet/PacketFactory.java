package com.kraken.api.core.packet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kraken.api.core.packet.model.LoginHooks;
import com.kraken.api.core.packet.model.MappedPackets;
import com.kraken.api.core.packet.model.PacketDefinition;
import com.kraken.api.core.packet.model.PacketMetadata;
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
    private static LoginHooks loginHooks = null;

    @Getter
    private static String clientVersion = "";

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final String LOCAL_PACKETS_PATH = "/packets.json";

    static {
        try {
            MappedPackets mappedPackets = JsonResourceUtils.loadJsonResource(
                    PacketFactory.class,
                    LOCAL_PACKETS_PATH,
                    gson,
                    MappedPackets.class
            );
            if (mappedPackets.getPackets() == null || mappedPackets.getLoginHooks() == null) {
                throw new IllegalStateException("Parsed packets or login hooks was null.");
            }

            packets = mappedPackets.getPackets();
            packetMetadata = mappedPackets.getReflectionHooks();
            clientVersion = mappedPackets.getClientVersion();
            loginHooks = mappedPackets.getLoginHooks();
            log.info("Loaded packets, reflection, and login hooks from local resources.");
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
    // pause is for basic "CLick here to continue", count is when entering a quantity like: withdraw-X, obj is when
    // picking an object i.e. sailing mermaids or picking an item from a GE search.
    public static PacketDefinition getResumeCountDialog() { return getPacket("RESUME_COUNTDIALOG"); }
    public static PacketDefinition getResumeObjDialog() { return getPacket("RESUME_OBJDIALOG"); }
    public static PacketDefinition getStringDialog() { return getPacket("RESUME_STRINGDIALOG"); }

    // Movement and event packets
    public static PacketDefinition getMoveGameClick() { return getPacket("MOVE_GAMECLICK"); }
    public static PacketDefinition getEventMouseClick() { return getPacket("EVENT_MOUSE_CLICK"); }
}
