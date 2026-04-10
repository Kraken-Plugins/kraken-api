package com.kraken.api.core.packet.entity;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.kraken.api.core.packet.PacketClient;
import com.kraken.api.core.packet.model.PacketFactory;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ItemComposition;
import net.runelite.api.widgets.Widget;
import net.runelite.client.util.Text;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A high-level utility class for sending widget-related game packets.
 * This class abstracts the complexity of constructing and sending
 * packets related to widget (interface) interactions, such as clicking buttons.
 * It uses a {@link PacketClient} provider to send the low-level packets,
 * which are defined by the {@link PacketFactory}.
 */
@Slf4j
@Singleton
public class WidgetPackets {

    @Inject
    private Provider<PacketClient> packetSenderProvider;

    /**
     * Queues a widget sub-action packet by identifying the specific sub-action
     * and menu options associated with a given widget.
     * <p>
     * This method identifies the indices of both a sub-action (from item definitions)
     * and a specific menu option (from the widget's actions). If matches for both
     * the sub-action and menu option are found, it sends a low-level packet to
     * perform the action.
     * <p>
     * Only executes if the widget and its associated item ID are valid, while the
     * sub-actions and menu options must contain the desired action and menu option.
     *
     * @param widget The {@link Widget} instance on which the action is to be performed.
     *               This is the target widget for the queued action.
     * @param menu   A case-insensitive {@literal @<String>} representing the menu action
     *               text to search for (e.g., "Use", "Examine").
     * @param action A case-insensitive {@literal @<String>} representing the sub-action text
     *               to search for (e.g., "Clean", "Equip").
     */
    @SneakyThrows
    public void queueWidgetSubAction(Widget widget, String menu, String action) {
        if (widget == null || widget.getItemId() == -1) {
            return;
        }

        ItemComposition composition = packetSenderProvider.get().getClient().getItemDefinition(widget.getItemId());
        String[][] subOps = composition.getSubops();
        List<String> actions = Arrays.stream(widget.getActions()).collect(Collectors.toList());

        int menuIndex = -1;
        int actionIndex = -1;

        if (subOps == null) {
            return;
        }

        for (String[] subOp : subOps) {
            if (actionIndex != -1) {
                break;
            }
            if (subOp != null) {
                for (int i = 0; i < subOp.length; i++) {
                    String op = subOp[i];
                    if (op != null && op.equalsIgnoreCase(action)) {
                        actionIndex = i;
                        break;
                    }
                }
            }
        }

        for (int i = 0; i < actions.size(); i++) {
            String a = actions.get(i);
            if (a != null && a.equalsIgnoreCase(menu)) {
                menuIndex = i + 1;
                break;
            }
        }

        if (menuIndex == -1 || actionIndex == -1) {
            String actionsString = actions.stream().filter(Objects::nonNull).map(Text::removeTags).collect(Collectors.joining(", "));
            log.error("No valid sub-action found for: {}, Actions: [{}]", action, actionsString);
            return;
        }

        packetSenderProvider.get()
                .sendPacket(PacketFactory.getIfSubOp(), widget.getId(), widget.getIndex(), widget.getItemId(), menuIndex, actionIndex);
    }

    /**
     * Queues the RESUME_PAUSEBUTTON packet, typically sent when the player
     * clicks a "Click here to continue" or "Close" button on a standard,
     * non-interactable dialog, such as a dialogue with an NPC. The widget id should be
     * a packed integer (containing both the group and child ids).
     *
     * @param widgetId The ID of the top-level widget (packed to include group and child ids).
     * @param childId The ID of the child component that was clicked.
     */
    public void queueResumePause(int widgetId, int childId) {
        packetSenderProvider.get().sendPacket(PacketFactory.getResumePausebutton(), widgetId, childId);
    }

    /**
     * Queues the RESUME_PAUSEBUTTON packet, usually sent when interacting with
     * non-interactable dialogs (e.g., "Click here to continue").
     *
     * <p>This method sends a packet using the provided packed widget ID.
     *
     * @param packed The packed widget ID, which includes both group and child IDs.
     */
    public void queueResumePause(int packed) {
        packetSenderProvider.get().sendPacket(PacketFactory.getResumePausebutton(), packed, -1);
    }

    /**
     * Queues the RESUME_COUNTDIALOG packet, sent in response to a numerical
     * input dialog (e.g., "How many?" or "Enter amount").
     *
     * @param id The integer value entered by the player.
     */
    public void queueResumeCount(int id) {
        packetSenderProvider.get().sendPacket(PacketFactory.getResumeCountDialog(), id);
    }

    /**
     * Queues the RESUME_OBJDIALOG packet, typically sent as a continuation
     * packet after selecting an option in a multi-choice dialog, where the
     * value represents an item ID or object ID relevant to the dialog option.
     *
     * @param value The numerical value associated with the dialog option.
     */
    public void queueResumeObj(int value) {
        packetSenderProvider.get().sendPacket(PacketFactory.getResumeObjDialog(), value);
    }
}
