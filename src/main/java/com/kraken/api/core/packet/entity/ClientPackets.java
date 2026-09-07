package com.kraken.api.core.packet.entity;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.kraken.api.core.packet.PacketClient;
import com.kraken.api.core.packet.PacketFactory;

import javax.inject.Singleton;

/**
 * The client packets class defines packets which are client specific and not related to any specific entity
 * in the game (like widgets, NPC's, players, objects, etc...)
 */
@Singleton
public class ClientPackets {

    @Inject
    private Provider<PacketClient> packetSenderProvider;


    /**
     * Queues an EVENT_APPLET_FOCUS packet indicating that the game client has gained focus.
     */
    public void queueAppletFocusGained() {
        packetSenderProvider.get().sendPacket(PacketFactory.getAppletFocus(), 1);
    }

    /**
     * Queues an EVENT_APPLES_FOCUS packet indicating that the game client has lost focus.
     */
    public void queueAppletFocusLost() {
        packetSenderProvider.get().sendPacket(PacketFactory.getAppletFocus(), 0);
    }
}
