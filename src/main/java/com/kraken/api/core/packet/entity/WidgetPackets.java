package com.kraken.api.core.packet.entity;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.kraken.api.core.packet.PacketClient;
import com.kraken.api.core.packet.model.PacketFactory;
import lombok.extern.slf4j.Slf4j;

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
