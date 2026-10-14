package unit.com.kraken.api.service.map;

import com.kraken.api.Context;
import com.kraken.api.service.map.RegionService;
import net.runelite.api.Client;
import net.runelite.api.WorldView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RegionServiceTest {
    private final Context ctx = mock(Context.class);
    private final Client client = mock(Client.class);
    private final WorldView worldView = mock(WorldView.class);
    private final RegionService service = new RegionService(() -> ctx);

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        when(ctx.getClient()).thenReturn(client);
        when(ctx.runOnClientThread(any(Callable.class), any()))
                .thenAnswer(invocation -> ((Callable<Object>) invocation.getArgument(0)).call());
        when(client.getTopLevelWorldView()).thenReturn(worldView);
    }

    @Test
    void matchesAnyLoadedRegion() {
        when(worldView.getMapRegions()).thenReturn(new int[]{5939, 6195});

        assertTrue(service.isInRegion(6195));
        assertTrue(service.isInRegion(1, 5939));
        assertFalse(service.isInRegion(1234));
    }

    @Test
    void matchesOnlyTheFirstLoadedRegion() {
        when(worldView.getMapRegions()).thenReturn(new int[]{5939, 6195});

        assertTrue(service.isFirstMapRegion(5939));
        assertFalse(service.isFirstMapRegion(6195));
    }

    @Test
    void matchesTheExactRegionsInOrder() {
        when(worldView.getMapRegions()).thenReturn(new int[]{5279, 5280, 5535, 5536});

        assertTrue(service.mapRegionsEqual(5279, 5280, 5535, 5536));
        assertFalse(service.mapRegionsEqual(5280, 5279, 5535, 5536));
        assertFalse(service.mapRegionsEqual(5279, 5280));
    }

    @Test
    void anEmptyRegionArrayMatchesNothing() {
        when(worldView.getMapRegions()).thenReturn(new int[0]);

        assertEquals(0, service.getMapRegions().length);
        assertFalse(service.isInRegion(5939));
        assertFalse(service.isFirstMapRegion(5939));
        assertFalse(service.mapRegionsEqual());
    }

    @Test
    void aMissingSceneMatchesNothing() {
        when(worldView.getMapRegions()).thenReturn(null);
        assertFalse(service.isInRegion(5939));

        when(client.getTopLevelWorldView()).thenReturn(null);
        assertFalse(service.isFirstMapRegion(5939));
        assertFalse(service.isInInstance());
    }

    @Test
    void returnsACopyOfTheRegions() {
        int[] regions = {5939, 6195};
        when(worldView.getMapRegions()).thenReturn(regions);

        service.getMapRegions()[0] = 1;

        assertArrayEquals(new int[]{5939, 6195}, regions);
    }

    @Test
    void reportsInstances() {
        when(worldView.isInstance()).thenReturn(true);
        assertTrue(service.isInInstance());

        when(worldView.isInstance()).thenReturn(false);
        assertFalse(service.isInInstance());
    }
}
