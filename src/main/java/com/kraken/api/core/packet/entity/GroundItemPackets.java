package com.kraken.api.core.packet.entity;


import com.kraken.api.core.packet.PacketClient;
import com.kraken.api.core.packet.model.PacketFactory;
import net.runelite.api.TileItem;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * A utility class for sending packets related to {@link TileItem} (ground item) interactions
 * to the game server.
 * <p>
 * This class primarily handles picking up ground items (actions) and using items/widgets
 * directly on a ground item. It abstracts the low-level packet construction and world
 * coordinate handling.
 */
public class GroundItemPackets {

    @Inject
    private Provider<PacketClient> packetClientProvider;

    /**
     * Queues the raw packet for using a widget (typically an item) on a ground item.
     * <p>
     * This method sends the {@code OPOBJT} (Use Widget on Object/Item) packet, which
     * includes the coordinates of the target ground item and the details of the source item/widget.
     *
     * @param objectId The Item ID of the ground item.
     * @param worldPointX The X coordinate of the item's location in the world.
     * @param worldPointY The Y coordinate of the item's location in the world.
     * @param sourceSlot The slot index of the item being used (e.g., inventory slot).
     * @param sourceItemId The ID of the item being used.
     * @param sourceWidgetId The ID of the parent widget containing the item (e.g., inventory widget ID).
     * @param ctrlDown If true, indicates the control key was held down.
     */
    public void queueWidgetOnGroundItem(int objectId, int worldPointX, int worldPointY, int sourceSlot, int sourceItemId, int sourceWidgetId, boolean ctrlDown) {
        int ctrl = ctrlDown ? 1 : 0;
        packetClientProvider.get().sendPacket(PacketFactory.getOpObjT(), objectId, worldPointX, worldPointY, sourceSlot, sourceItemId,
                sourceWidgetId, ctrl);
    }

    /**
     * Queues the packet for using a specific {@link Widget} (or item it represents) on a target {@link TileItem}.
     * <p>
     * This is a convenience method that simplifies sending the {@code OPOBJT} packet
     * by extracting necessary details from the provided {@link TileItem} and source {@link Widget}.
     *
     * @param item The target {@link TileItem} object on the ground.
     * @param location The {@link WorldPoint} location of the item on the map.
     * @param w The source {@link Widget} containing the item or action to be used on the ground item.
     * @param ctrlDown If true, indicates the control key was held down.
     */
    public void queueWidgetOnGroundItem(TileItem item, WorldPoint location, Widget w, boolean ctrlDown) {
        queueWidgetOnGroundItem(item.getId(), location.getX(), location.getY(), w.getIndex(), w.getItemId(), w.getId(), ctrlDown);
    }
}