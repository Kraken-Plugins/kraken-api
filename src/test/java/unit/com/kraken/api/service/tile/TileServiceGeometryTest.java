package unit.com.kraken.api.service.tile;

import com.google.inject.Provider;
import com.kraken.api.Context;
import com.kraken.api.service.tile.TileService;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TileServiceGeometryTest {
    private final TileService service = new TileService();
    private final Context ctx = mock(Context.class);
    private final Client client = mock(Client.class);
    private final WorldView view = mock(WorldView.class);
    private final Scene scene = mock(Scene.class);
    private final Player player = mock(Player.class);
    private final CollisionData collision = mock(CollisionData.class);
    private final int[][] flags = new int[104][104];

    @BeforeEach
    void setup() throws Exception {
        Field provider = TileService.class.getDeclaredField("ctxProvider");
        provider.setAccessible(true);
        provider.set(service, (Provider<Context>) () -> ctx);
        when(ctx.getClient()).thenReturn(client);
        when(ctx.runOnClientThread(any(Callable.class))).thenAnswer(call -> ((Callable<?>) call.getArgument(0)).call());
        when(client.getTopLevelWorldView()).thenReturn(view);
        when(client.getLocalPlayer()).thenReturn(player);
        when(player.getWorldView()).thenReturn(view);
        when(view.getScene()).thenReturn(scene);
        when(view.getSizeX()).thenReturn(104);
        when(view.getSizeY()).thenReturn(104);
        when(view.getCollisionMaps()).thenReturn(new CollisionData[]{collision, collision});
        when(collision.getFlags()).thenReturn(flags);
        when(view.getBaseX()).thenReturn(3200);
        when(view.getBaseY()).thenReturn(3200);
        when(scene.getBaseX()).thenReturn(3200);
        when(scene.getBaseY()).thenReturn(3200);
        // Block everything except tiles opened by the test so alternate routes cannot mask a wall.
        for (int[] row : flags) Arrays.fill(row, CollisionDataFlag.BLOCK_MOVEMENT_FULL);
        movePlayer(20, 20);
    }

    @Test
    void liveBoundsHandleRotatedTwoByThreeAndEvenSizedObjects() {
        GameObject object = object(21, 19, 22, 21);
        when(object.getOrientation()).thenReturn(512);
        // The center rounds onto scene tile 22; reconstructing the SW corner from size shifts it.
        when(object.getLocalLocation()).thenAnswer(call -> LocalPoint.fromScene(22, 20, view));
        assertTrue(service.isObjectReachable(object));
        verify(client, never()).getObjectDefinition(anyInt());
        when(object.getSceneMaxLocation()).thenReturn(new Point(23, 20));
        assertTrue(service.isObjectReachable(object));
    }

    @Test
    void cornerContactDoesNotCountAsAnApproach() {
        assertFalse(service.isObjectReachable(object(21, 21, 22, 23)));
    }

    @Test
    void wallsOnEitherSideOfTheApproachAreRespectedWithinTheSameTick() {
        GameObject object = object(21, 20, 22, 22);
        assertTrue(service.isObjectReachable(object));
        flags[20][20] |= CollisionDataFlag.BLOCK_MOVEMENT_EAST;
        assertFalse(service.isObjectReachable(object));
        flags[20][20] = 0;
        flags[21][20] |= CollisionDataFlag.BLOCK_MOVEMENT_WEST;
        assertFalse(service.isObjectReachable(object));
    }

    @Test
    void walkableTileChecksTheDestinationSideOfAWall() {
        flags[21][20] = CollisionDataFlag.BLOCK_MOVEMENT_WEST;
        assertFalse(service.isTileReachable(point(21, 20, 0)));
        flags[21][20] = 0;
        assertTrue(service.isTileReachable(point(21, 20, 0)));
    }

    @Test
    void playerMovementPlaneAndCollisionReplacementInvalidateTheCache() {
        WorldPoint target = point(20, 20, 0);
        assertTrue(service.isTileReachable(target));
        movePlayer(50, 50);
        assertFalse(service.isTileReachable(target));
        movePlayer(20, 20);
        assertTrue(service.isTileReachable(target));
        when(view.getPlane()).thenReturn(1);
        assertFalse(service.isTileReachable(target));
        assertTrue(service.isTileReachable(point(20, 20, 1)));
        int[][] replacement = new int[104][104];
        when(collision.getFlags()).thenReturn(replacement);
        assertTrue(service.isTileReachable(point(50, 50, 1)));
    }

    @Test
    void sceneBaseAndWorldViewChangesDoNotReuseStaleCoordinates() {
        assertTrue(service.isTileReachable(point(20, 20, 0)));
        when(view.getBaseX()).thenReturn(3300);
        assertFalse(service.isTileReachable(point(20, 20, 0)));
        when(view.getScene()).thenReturn(mock(Scene.class));
        assertTrue(service.isTileReachable(new WorldPoint(3320, 3220, 0)));
        when(player.getWorldView()).thenReturn(mock(WorldView.class));
        assertFalse(service.isTileReachable(new WorldPoint(3320, 3220, 0)));
        assertFalse(service.isObjectReachable(object(21, 20, 22, 22)));
    }

    @Test
    void instanceConversionsRespectTemplatePlaneAndRepeatedChunks() {
        int[][][] chunks = new int[4][13][13];
        for (int[][] plane : chunks) for (int[] row : plane) Arrays.fill(row, -1);
        // Two occurrences of template (4000, 4000, 1), one rotated.
        chunks[0][2][2] = (1 << 24) | (500 << 14) | (500 << 3);
        chunks[0][6][6] = (1 << 24) | (500 << 14) | (500 << 3) | (1 << 1);
        when(scene.isInstance()).thenReturn(true);
        when(view.isInstance()).thenReturn(true);
        when(scene.getInstanceTemplateChunks()).thenReturn(chunks);
        when(view.getInstanceTemplateChunks()).thenReturn(chunks);
        WorldPoint template = new WorldPoint(4001, 4002, 1);
        assertEquals(2, service.toInstance(template).size());
        assertNotNull(service.fromWorldInstance(template));
        assertNull(service.fromWorldInstance(new WorldPoint(4001, 4002, 0)));
        assertTrue(service.toInstance(new WorldPoint(4001, 4002, 0)).isEmpty());
        for (WorldPoint occurrence : service.toInstance(template)) {
            assertEquals(template, service.fromInstance(occurrence));
        }
        // Only the second occurrence is accessible; tile reachability must inspect all matches.
        WorldPoint second = service.toInstance(template).get(1);
        movePlayer(second.getX() - 3200, second.getY() - 3200);
        assertTrue(service.isTileReachable(template));
        assertFalse(service.isTileReachable(new WorldPoint(4001, 4002, 0)));
    }

    @Test
    void smallerSecondaryViewUsesItsOwnCollisionDimensions() {
        int[][] smallFlags = new int[32][32];
        when(collision.getFlags()).thenReturn(smallFlags);
        when(view.getId()).thenReturn(42);
        when(view.getSizeX()).thenReturn(32);
        when(view.getSizeY()).thenReturn(32);
        movePlayer(20, 20);
        assertTrue(service.isObjectReachable(object(25, 25, 26, 27)));
        assertFalse(service.isObjectReachable(object(32, 25, 33, 27)));
    }

    private void movePlayer(int x, int y) {
        flags[x][y] = 0;
        when(player.getLocalLocation()).thenAnswer(call -> LocalPoint.fromScene(x, y, view));
    }

    private GameObject object(int minX, int minY, int maxX, int maxY) {
        GameObject object = mock(GameObject.class);
        when(object.getWorldView()).thenReturn(view);
        when(object.getSceneMinLocation()).thenReturn(new Point(minX, minY));
        when(object.getSceneMaxLocation()).thenReturn(new Point(maxX, maxY));
        return object;
    }

    private WorldPoint point(int x, int y, int plane) {
        return new WorldPoint(3200 + x, 3200 + y, plane);
    }
}
