package unit.com.kraken.api.query.groundobject;

import com.kraken.api.Context;
import com.kraken.api.query.groundobject.GroundItem;
import com.kraken.api.query.groundobject.GroundObjectQuery;
import net.runelite.api.GroundObject;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemLayer;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.game.ItemManager;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Regression coverage for RuneLite 1.13.0 long item prices preserving Kraken's public int gePrice API.
 */
class GroundObjectQueryPriceSaturationTest {

    @Test
    void representablePriceRemainsExactInt() throws Exception {
        GroundItem item = buildGroundItemWithGePrice(123_456_789L);
        assertEquals(123_456_789, item.getGePrice());
    }

    @Test
    void aboveIntMaxPriceSaturatesInsteadOfWrappingNegative() throws Exception {
        GroundItem item = buildGroundItemWithGePrice((long) Integer.MAX_VALUE + 42L);
        assertEquals(Integer.MAX_VALUE, item.getGePrice());
    }

    @SuppressWarnings("unchecked")
    private static GroundItem buildGroundItemWithGePrice(long gePrice) throws Exception {
        Context ctx = mock(Context.class);
        ItemManager itemManager = mock(ItemManager.class);
        when(ctx.getItemManager()).thenReturn(itemManager);
        when(ctx.runOnClientThreadOptional(any(Callable.class))).thenReturn(Optional.of(0));

        Tile tile = mock(Tile.class);
        TileItem tileItem = mock(TileItem.class);
        GroundObject tileObject = mock(GroundObject.class);
        ItemLayer itemLayer = mock(ItemLayer.class);
        ItemComposition composition = mock(ItemComposition.class);

        when(tile.getWorldLocation()).thenReturn(new WorldPoint(3200, 3200, 0));
        when(tile.getGroundObject()).thenReturn(tileObject);
        when(tile.getItemLayer()).thenReturn(itemLayer);
        when(itemLayer.getHeight()).thenReturn(0);

        when(tileItem.getId()).thenReturn(1001);
        when(tileItem.getQuantity()).thenReturn(1);
        when(tileItem.getDespawnTime()).thenReturn(100);
        when(tileItem.getVisibleTime()).thenReturn(100);
        when(tileItem.getOwnership()).thenReturn(0);
        when(tileItem.isPrivate()).thenReturn(false);

        when(composition.getNote()).thenReturn(-1);
        when(composition.getLinkedNoteId()).thenReturn(1001);
        when(composition.getHaPrice()).thenReturn(5);
        when(composition.getName()).thenReturn("TEST_ITEM");
        when(composition.isTradeable()).thenReturn(true);
        when(composition.isStackable()).thenReturn(false);
        when(itemManager.getItemComposition(1001)).thenReturn(composition);
        when(itemManager.getItemPrice(1001)).thenReturn(gePrice);

        GroundObjectQuery query = new GroundObjectQuery(ctx);
        Method build = GroundObjectQuery.class.getDeclaredMethod("buildGroundItem", Tile.class, TileItem.class);
        build.setAccessible(true);
        return (GroundItem) build.invoke(query, tile, tileItem);
    }
}
