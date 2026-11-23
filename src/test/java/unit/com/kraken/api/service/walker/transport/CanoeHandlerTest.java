package unit.com.kraken.api.service.walker.transport;

import com.kraken.api.Context;
import com.kraken.api.service.walker.transport.ObjectInfo;
import com.kraken.api.service.walker.transport.TransportContext;
import com.kraken.api.service.walker.transport.handler.CanoeHandler;
import net.runelite.api.coords.WorldPoint;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Covers the canoes the handler refuses before it touches the game: one that names no stop to
 * paddle to, and one that names no station to build at. Reading a station's stage and choosing
 * from the interfaces are covered by {@link CanoeStageTest} and {@link CanoeWidgetsTest}.
 */
class CanoeHandlerTest {

    private static final WorldPoint STATION = new WorldPoint(3243, 3237, 0);
    private static final ObjectInfo CANOE_STATION = ObjectInfo.parse("Paddle Canoe Canoe Station 12163");

    private final Context ctx = mock(Context.class);
    private final CanoeHandler handler = new CanoeHandler();

    @Test
    void aCanoeThatNamesNoStopIsNotBuilt() {
        TransportContext context = TransportContext.builder()
                .ctx(ctx).origin(STATION).objectInfo(CANOE_STATION).displayInfo(" ").build();

        assertFalse(handler.execute(context));
        assertTrue(context.getFailureReason().contains("no stop"), context.getFailureReason());
        verifyNoInteractions(ctx);
    }

    @Test
    void aCanoeThatNamesNoStationIsNotBuilt() {
        TransportContext noStation = TransportContext.builder()
                .ctx(ctx).origin(STATION).displayInfo("Edgeville").build();
        TransportContext noOrigin = TransportContext.builder()
                .ctx(ctx).objectInfo(CANOE_STATION).displayInfo("Edgeville").build();

        assertFalse(handler.execute(noStation));
        assertFalse(handler.execute(noOrigin));
        assertTrue(noStation.getFailureReason().contains("no station"), noStation.getFailureReason());
        assertTrue(noOrigin.getFailureReason().contains("no station"), noOrigin.getFailureReason());
        verifyNoInteractions(ctx);
    }
}
