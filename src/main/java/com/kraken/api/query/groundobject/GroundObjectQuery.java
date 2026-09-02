package com.kraken.api.query.groundobject;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractSpatialQuery;
import net.runelite.api.ItemComposition;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.client.util.RSTimeUnit;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class GroundObjectQuery extends AbstractSpatialQuery<GroundObjectEntity, GroundObjectQuery, GroundItem> {
    private static final int COINS = 617;

    public GroundObjectQuery(Context ctx) {
        super(ctx);
    }

    /**
     * Builds a GroundItem object from a Tile and TileItem. The tile item is the actual object that is on the ground
     * however, data from the tile is also necessary to build a full representation
     *
     * @param tile The tile the item is on
     * @param item The tile item which contains information about the item on the ground
     * @return A GroundItem object which represents the item on the ground
     */
    private GroundItem buildGroundItem(final Tile tile, final TileItem item) {
        int tickCount = ctx.runOnClientThreadOptional(() -> ctx.getClient().getTickCount()).orElse(0);
        final int itemId = item.getId();
        final ItemComposition itemComposition = ctx.getItemManager().getItemComposition(itemId);
        final int realItemId = itemComposition.getNote() != -1 ? itemComposition.getLinkedNoteId() : itemId;
        final int alchPrice = itemComposition.getHaPrice();
        final int despawnTime = item.getDespawnTime() - tickCount;
        final int visibleTime = item.getVisibleTime() - tickCount;
        final String key = String.format("%d-%d-%d-%d-%d",
                item.getId(),
                item.getQuantity(),
                tile.getWorldLocation().getX(),
                tile.getWorldLocation().getY(),
                tile.getPlane());

        final GroundItem groundItem = GroundItem.builder()
                .id(itemId)
                .key(key)
                .itemComposition(itemComposition)
                .tileItem(item)
                .tileObject(tile.getGroundObject())
                .location(tile.getWorldLocation())
                .itemId(realItemId)
                .quantity(item.getQuantity())
                .name(itemComposition.getName())
                .haPrice(alchPrice)
                .height(tile.getItemLayer().getHeight())
                .tradeable(itemComposition.isTradeable())
                .ownership(item.getOwnership())
                .isPrivate(item.isPrivate())
                .spawnTime(Instant.now())
                .stackable(itemComposition.isStackable())
                .despawnTime(Duration.of(despawnTime, RSTimeUnit.GAME_TICKS))
                .visibleTime(Duration.of(visibleTime, RSTimeUnit.GAME_TICKS))
                .build();

        if (realItemId == COINS) {
            groundItem.setHaPrice(1);
            groundItem.setGePrice(1);
        } else {
            groundItem.setGePrice(ctx.getItemManager().getItemPrice(realItemId));
        }

        return groundItem;
    }


    @Override
    protected Supplier<Stream<GroundObjectEntity>> source() {
        return () -> {
            HashSet<GroundItem> groundItems = new HashSet<>();
            for (Tile[] tiles : ctx.getClient().getTopLevelWorldView().getScene().getTiles()[ctx.getClient().getTopLevelWorldView().getPlane()]) {
                if (tiles == null) {
                    continue;
                }

                for (Tile tile : tiles) {
                    if (tile == null) {
                        continue;
                    }

                    if (tile.getGroundItems() != null) {
                        for (TileItem groundItem : tile.getGroundItems()) {
                            if (groundItem == null) {
                                continue;
                            }
                            groundItems.add(buildGroundItem(tile, groundItem));
                        }
                    }
                }
            }

            return groundItems.stream().map(groundItem -> new GroundObjectEntity(ctx, groundItem));
        };
    }

    /**
     * Filters the stream of ground objects to only objects which have been dropped by the local player (you).
     * @return GroundObjectQuery
     */
    public GroundObjectQuery ownedByLocalPlayer() {
        return filter(obj -> obj.raw().isOwnedByLocalPlayer());
    }

    /**
     * Filters the stream of ground objects to only objects which have been dropped by other players.
     * @return GroundObjectQuery
     */
    public GroundObjectQuery ownedByOthers() {
        return filter(obj -> !obj.raw().isOwnedByLocalPlayer());
    }

    /**
     * Filters for ground items where the Grand Exchange price is above a specific threshold. Stacks of items are NOT
     * considered with this method. Use {@code stackValueAbove()} to consider stacks of items.
     * @param value The value threshold for the items.
     * @return GroundObjectQuery
     */
    public GroundObjectQuery valueAbove(int value) {
        return filter(obj -> obj.raw().getGePrice() > value);
    }

    /**
     * Filters for ground items where the Grand Exchange price is above a specific threshold. Stacks of items are taken
     * into consideration when using this method.
     * @param value The value threshold for the items.
     * @return GroundObjectQuery
     */
    public GroundObjectQuery stackValueAbove(int value) {
        return filter(obj -> obj.raw().getGrandExchangePrice() > value);
    }

    /**
     * Filters for ground items where the high alchemy price is above a specific threshold.
     * @param value The value threshold for the ground items
     * @return GroundObjectQuery
     */
    public GroundObjectQuery highAlchemyPriceAbove(int value) {
        return filter(obj -> obj.raw().getHaPrice() > value);
    }

}

