package com.kraken.api.core.packet;

import com.kraken.api.core.hooks.HooksLoader;
import com.kraken.api.core.packet.model.PacketDefinition;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>A factory class for creating and managing {@link PacketDefinition} instances for various packet types and actions.
 * This class initializes packet definitions by fetching and parsing a JSON configuration file locally first,
 * then falling back to a remote URL.</p>
 */
@Slf4j
public class PacketFactory {

    /**
     * Helper method to safely retrieve a packet and fail fast if the JSON is missing definitions.
     */
    private static PacketDefinition getPacket(String packetName) {
        PacketDefinition def = HooksLoader.getPackets().get(packetName);
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
