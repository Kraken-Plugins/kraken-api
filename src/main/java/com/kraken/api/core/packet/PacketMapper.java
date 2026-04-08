package com.kraken.api.core.packet;

import com.kraken.api.core.interceptor.model.PacketSent;
import com.kraken.api.core.packet.model.PacketFactory;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.KeyCode;
import net.runelite.api.MenuAction;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.eventbus.Subscribe;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Singleton
public class PacketMapper {

    @Inject
    private Client client;

    @Inject
    private PacketFactory packetFactory;

    /**
     * Seeds the interceptor with expected values right before the packet is built.
     */
    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event) {
        if (event.getMenuAction() == MenuAction.GAME_OBJECT_FIRST_OPTION) {
            Map<String, Integer> expectedArgs = new HashMap<>();
            expectedArgs.put("objectId", event.getId());
            WorldPoint pt = WorldPoint.fromScene(client.getTopLevelWorldView().getScene(), event.getParam0(), event.getParam1(), client.getTopLevelWorldView().getPlane());
            expectedArgs.put("worldPointX", pt.getX());
            expectedArgs.put("worldPointY", pt.getY());
            expectedArgs.put("ctrlDown", client.isKeyPressed(KeyCode.KC_CONTROL) ? 1 : 0);
            expectedArgs.put("subop", 0);

            log.info("OPOBJ1 ({}) Template: {}", PacketFactory.getPackets().get("OPOBJ1").getWrites().length, expectedArgs);
        }
    }

    @Subscribe
    private void onPacketSent(PacketSent e) {
        log.info("{}", e);
        String decoded = BufferReader.debugDecode("OPOBJ1", e.getPacket().getPayload(), Arrays.asList(packetFactory.getOpObj1().getWrites()));
        log.info("Decoded: {}", decoded);
        // EncodedPacket(encodedId=34592096, encodedLength=-622572408, payload=[111, 96, 13, 0, 68, 42, -128, 12, -47, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0], packetBufferNode=wg@48f6b03e)
    }
}
