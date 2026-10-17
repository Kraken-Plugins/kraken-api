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
    /**
     * Returns the definition of the packet that answers a dialogue asking for a quantity.
     * @return The RESUME_COUNTDIALOG packet definition.
     */
    public static PacketDefinition getResumeCountDialog() { return getPacket("RESUME_COUNTDIALOG"); }

    /**
     * Returns the definition of the packet that picks an object or item from a dialogue.
     * @return The RESUME_OBJDIALOG packet definition.
     */
    public static PacketDefinition getResumeObjDialog() { return getPacket("RESUME_OBJDIALOG"); }

    /**
     * Returns the definition of the packet that answers a dialogue asking for a string.
     * @return The RESUME_STRINGDIALOG packet definition.
     */
    public static PacketDefinition getStringDialog() { return getPacket("RESUME_STRINGDIALOG"); }

    /**
     * Returns the definition of the packet that reports the applet's focus state.
     * @return The EVENT_APPLET_FOCUS packet definition.
     */
    public static PacketDefinition getAppletFocus() {
        return getPacket("EVENT_APPLET_FOCUS");
    }

    /**
     * Returns the definition of the packet sent when the player clicks a scene tile to walk there.
     * @return The MOVE_GAMECLICK packet definition.
     */
    public static PacketDefinition getMoveGameClick() { return getPacket("MOVE_GAMECLICK"); }

    /**
     * Returns the definition of the packet sent for any mouse click on the client.
     * @return The EVENT_MOUSE_CLICK packet definition.
     */
    public static PacketDefinition getEventMouseClick() { return getPacket("EVENT_MOUSE_CLICK"); }
}
