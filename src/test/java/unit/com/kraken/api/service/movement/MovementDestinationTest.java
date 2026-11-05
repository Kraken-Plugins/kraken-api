package unit.com.kraken.api.service.movement;

import com.kraken.api.Context;
import com.kraken.api.service.movement.MovementService;
import com.kraken.api.service.tile.TileService;
import net.runelite.api.Client;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Covers how {@link MovementService} resolves a destination to the scene coordinates the movement packet carries.
 * The loaded scene is an instance whose base is (6400, 6400) on plane 0; its template tiles are elsewhere on the map.
 */
class MovementDestinationTest {

    private static final int BASE_X = 6400;
    private static final int BASE_Y = 6400;
    private static final int WORLD_VIEW_ID = -1;

    private final MovementService service = new MovementService();
    private final Context ctx = mock(Context.class);
    private final Client client = mock(Client.class);
    private final WorldView view = mock(WorldView.class);
    private final TileService tileService = mock(TileService.class);

    @BeforeEach
    void setup() throws Exception {
        inject("ctx", ctx);
        inject("tileService", tileService);
        when(ctx.getClient()).thenReturn(client);
        when(client.getTopLevelWorldView()).thenReturn(view);
        when(client.getWorldView(WORLD_VIEW_ID)).thenReturn(view);
        when(view.getId()).thenReturn(WORLD_VIEW_ID);
        when(view.getBaseX()).thenReturn(BASE_X);
        when(view.getBaseY()).thenReturn(BASE_Y);
        when(view.getSizeX()).thenReturn(104);
        when(view.getSizeY()).thenReturn(104);
        when(view.getPlane()).thenReturn(0);
        when(view.isInstance()).thenReturn(true);
    }

    private void inject(String field, Object value) throws Exception {
        Field declared = MovementService.class.getDeclaredField(field);
        declared.setAccessible(true);
        declared.set(service, value);
    }

    @Test
    void outsideAnInstanceThePointIsSentUnchanged() {
        when(view.isInstance()).thenReturn(false);
        WorldPoint point = new WorldPoint(3200, 3200, 0);

        assertEquals(point, service.toSceneDestination(point));
    }

    @Test
    void aTemplatePointIsConvertedToItsTileInTheScene() {
        WorldPoint template = new WorldPoint(3550, 5150, 0);
        LocalPoint occurrence = LocalPoint.fromScene(10, 12, view);
        when(tileService.fromWorldInstance(template)).thenReturn(occurrence);

        assertEquals(new WorldPoint(BASE_X + 10, BASE_Y + 12, 0), service.toSceneDestination(template));
    }

    @Test
    void aPointAlreadyInTheSceneIsSentUnchanged() {
        // An actor's world location inside an instance. It is not a template point, so the tile service finds no
        // template occurrence, and the point must not be converted to its template point before it is sent.
        WorldPoint scenePoint = new WorldPoint(BASE_X + 30, BASE_Y + 40, 0);
        when(tileService.fromWorldInstance(any())).thenReturn(null);

        assertEquals(scenePoint, service.toSceneDestination(scenePoint));
    }

    @Test
    void aPointThatIsNeitherATemplatePointNorInTheSceneIsRejected() {
        when(tileService.fromWorldInstance(any())).thenReturn(null);

        assertNull(service.toSceneDestination(new WorldPoint(BASE_X + 200, BASE_Y, 0)));
    }

    @Test
    void aScenePointOnAnotherPlaneIsRejected() {
        when(tileService.fromWorldInstance(any())).thenReturn(null);

        assertNull(service.toSceneDestination(new WorldPoint(BASE_X + 30, BASE_Y + 40, 1)));
    }

    @Test
    void aLocalPointResolvesToItsSceneTileInsideAnInstance() {
        assertEquals(new WorldPoint(BASE_X + 5, BASE_Y + 7, 0), service.toSceneDestination(LocalPoint.fromScene(5, 7, view)));
    }

    @Test
    void nullDestinationsAreRejected() {
        assertNull(service.toSceneDestination((WorldPoint) null));
        assertNull(service.toSceneDestination((LocalPoint) null));
    }
}
