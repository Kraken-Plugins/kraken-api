package com.kraken.api.core.packet.entity;

import com.google.inject.Provider;
import com.kraken.api.Context;
import com.kraken.api.core.packet.PacketClient;
import com.kraken.api.core.packet.model.PacketDefFactory;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.widgets.Widget;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A static utility class for sending packets related to Non-Player Character (NPC) interactions
 * to the game server.
 * <p>
 * This class handles various forms of NPC interaction, including standard action clicks
 * (e.g., Talk-to, Attack) and "use-with" actions (e.g., using an item on an NPC).
 * It uses a {@link PacketDefFactory} to determine the correct packet type and a
 * {@link PacketClient} to send the raw data.
 */
@Slf4j
public class NPCPackets {

    @Inject
    private PacketDefFactory packetDefFactory;

    @Inject
    private Provider<PacketClient> packetClientProvider;

    @Inject
    private Provider<Context> ctxProvider;

    /**
     * Queues the low-level packet to perform a generic action click on an NPC.
     * <p>
     * This method sends one of the {@code OPNPC} packets (e.g., {@code OPNPC1} through {@code OPNPC10}),
     * where the action is determined by the {@code actionFieldNo}.
     *
     * @param actionFieldNo The 1-based index of the action to execute (1-10).
     * @param npcIndex The server index of the target NPC.
     * @param ctrlDown If true, indicates the control key was held down (often used for force-attacking/force-clicking).
     */
    @SneakyThrows
    public void queueNPCAction(int actionFieldNo, int npcIndex, boolean ctrlDown) {
        int ctrl = ctrlDown ? 1 : 0;
        int subop = 0;
        packetClientProvider.get().sendPacket(packetDefFactory.getOpNpc(actionFieldNo), npcIndex, ctrl, subop);
    }

    /**
     * Queues an NPC action by matching a human-readable action string (e.g., "Talk-to", "Attack").
     * <p>
     * This is a high-level convenience method that inspects the target NPC's composition
     * for a matching action and automatically determines the correct low-level action number
     * (1-10) to use for the {@code OPNPC} packet. The search is case-insensitive.
     *
     * @param npc The target {@link NPC} object to interact with.
     * @param actionList A varargs list of action strings to search for. The first match found will be executed.
     */
    @SneakyThrows
    public void queueNPCAction(NPC npc, String... actionList) {
        if (npc == null) return;

        Context ctx = ctxProvider.get();
        NPCComposition comp = ctx.runOnClientThread(npc::getComposition);
        if (comp == null || comp.getActions() == null) return;

        List<String> actions = Arrays.asList(comp.getActions());

        if (actions.stream().allMatch(Objects::isNull)) {
            NPCComposition transformed = ctx.runOnClientThread(npc::getTransformedComposition);
            if (transformed == null || transformed.getActions() == null) return;

            actions = Arrays.asList(transformed.getActions());
            if (actions.stream().allMatch(Objects::isNull)) {
                log.error("All NPC actions and transformed actions are null. No action to take on NPC: {}", npc.getName());
                return;
            }
        }

        Set<String> targets = Arrays.stream(actionList)
                .map(s -> s.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        int num = -1;
        for (int i = 0; i < actions.size(); i++) {
            String action = actions.get(i);
            if (action != null && targets.contains(action.toLowerCase(Locale.ROOT))) {
                num = i + 1;
                log.debug("[NPC Packets] Found NPC action: {} at index: {}", action, num);
                break;
            }
        }

        if (num < 1 || num > 10) return;
        queueNPCAction(num, npc.getIndex(), false);
    }

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
        packetClientProvider.get().sendPacket(packetDefFactory.getOpNpcT(), npcIndex, sourceItemId, sourceSlot, sourceWidgetId, ctrl);
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

