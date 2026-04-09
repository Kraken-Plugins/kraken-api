package com.kraken.api.core.packet.entity;

import com.google.inject.Provider;
import com.kraken.api.core.packet.PacketClient;
import com.kraken.api.core.packet.model.PacketFactory;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.NPC;
import net.runelite.api.widgets.Widget;

import javax.inject.Inject;

/**
 * A static utility class for sending packets related to Non-Player Character (NPC) interactions
 * to the game server.
 * <p>
 * This class handles various forms of NPC interaction, including standard action clicks
 * (e.g., Talk-to, Attack) and "use-with" actions (e.g., using an item on an NPC).
 * It uses a {@link PacketFactory} to determine the correct packet type and a
 * {@link PacketClient} to send the raw data.
 */
@Slf4j
public class NPCPackets {

    @Inject
    private Provider<PacketClient> packetClientProvider;

    /**
     * Queues the raw packet for using a widget (typically an item) on an NPC.
     * <p>
     * This method sends the {@code OPNPCT} (Use Widget on NPC) packet, which
     * contains the details of the source item/widget and the target NPC.
     *
     * @param npcIndex The server index of the target NPC.
     * @param sourceItemId The ID of the item being used.
     * @param sourceSlot The slot index of the item being used (e.g., inventory slot).
     * @param sourceWidgetId The ID of the parent widget containing the item (e.g., inventory widget ID).
     * @param ctrlDown If true, indicates the control key was held down.
     */
    public void queueWidgetOnNPC(int npcIndex, int sourceItemId, int sourceSlot, int sourceWidgetId, boolean ctrlDown) {
        int ctrl = ctrlDown ? 1 : 0;
        packetClientProvider.get().sendPacket(PacketFactory.getOpNpcT(), npcIndex, sourceItemId, sourceSlot, sourceWidgetId, ctrl);
    }

    /**
     * Queues the packet for using a specific {@link Widget} (or item it represents) on a target {@link NPC}.
     * <p>
     * This is a convenience method that extracts the necessary item and widget details
     * from the provided {@link Widget} object and calls the raw {@code queueWidgetOnNPC} method.
     *
     * @param npc The target {@link NPC} object.
     * @param widget The source {@link Widget} containing the item or action to be used on the NPC.
     */
    public void queueWidgetOnNPC(NPC npc, Widget widget) {
        queueWidgetOnNPC(npc.getIndex(), widget.getItemId(), widget.getIndex(), widget.getId(), false);
    }
}

