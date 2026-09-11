package plugins.api.tests.service;

import com.kraken.api.Context;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import plugins.api.requirements.InventoryPolicy;
import plugins.api.requirements.SideEffect;
import plugins.api.world.NamedLocation;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TileServiceLiveTargetTest {
    private final TileServiceTest test = new TileServiceTest();
    private final Context ctx = mock(Context.class);
    private final Client client = mock(Client.class);
    private final WorldView view = mock(WorldView.class);
    private final Scene scene = mock(Scene.class);
    private final Player player = mock(Player.class);
    private final int[][] flags = new int[104][104];

    @BeforeEach
    void setup() {
        when(ctx.getClient()).thenReturn(client);
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(client.getTopLevelWorldView()).thenReturn(view);
        when(client.getLocalPlayer()).thenReturn(player);
        when(player.getWorldLocation()).thenReturn(new WorldPoint(3220, 3220, 0));
        when(player.getLocalLocation()).thenAnswer(call -> net.runelite.api.coords.LocalPoint.fromScene(20, 20, view));
        when(view.getScene()).thenReturn(scene);
        when(view.getSizeX()).thenReturn(104);
        when(view.getSizeY()).thenReturn(104);
        when(view.getBaseX()).thenReturn(3200);
        when(view.getBaseY()).thenReturn(3200);
        when(scene.getBaseX()).thenReturn(3200);
        when(scene.getBaseY()).thenReturn(3200);
        CollisionData collision = mock(CollisionData.class);
        when(collision.getFlags()).thenReturn(flags);
        when(view.getCollisionMaps()).thenReturn(new CollisionData[]{collision});
        for (int[] row : flags) Arrays.fill(row, CollisionDataFlag.BLOCK_MOVEMENT_FULL);
        for (int x = 20; x <= 23; x++) flags[x][20] = 0;
    }

    @Test
    void destinationRequiresRealDisplacementAlongAClearCorridor() {
        TileServiceTest.MoveTarget target = test.chooseTarget(ctx, false);
        assertNotNull(target);
        assertEquals(new WorldPoint(3223, 3220, 0), target.getScenePoint());
        assertEquals(target.getScenePoint(), target.getTemplate());
        assertNotEquals(target.getStart(), target.getScenePoint());
    }

    @Test
    void separatingWallCannotBeSelectedAsMovementTarget() {
        flags[21][20] |= CollisionDataFlag.BLOCK_MOVEMENT_WEST;
        assertNull(test.chooseTarget(ctx, false));
    }

    @Test
    void rotatedInstanceTargetPreservesTemplatePlaneAndLiveDestination() {
        int[][][] chunks = instanceChunks();
        chunks[0][2][2] = (1 << 24) | (500 << 14) | (500 << 3) | (1 << 1);
        TileServiceTest.MoveTarget target = test.chooseTarget(ctx, true);
        assertNotNull(target);
        assertEquals(new WorldPoint(3223, 3220, 0), target.getScenePoint());
        assertEquals(1, target.getTemplate().getPlane());
        assertNotEquals(target.getScenePoint(), target.getTemplate());
        assertTrue(WorldPoint.toLocalInstance(view, target.getTemplate()).contains(target.getScenePoint()));
    }

    @Test
    void repeatedChunksCannotSendTheMovementCheckToADifferentOccurrence() {
        int[][][] chunks = instanceChunks();
        chunks[0][1][1] = (500 << 14) | (500 << 3);
        chunks[0][2][2] = chunks[0][1][1];
        assertNull(test.chooseTarget(ctx, true));
    }

    @Test
    void staleOrMissingSceneCannotProduceATarget() {
        assertNull(test.chooseTarget(ctx, true));
        when(client.getGameState()).thenReturn(GameState.LOADING);
        assertNull(test.chooseTarget(ctx, false));
    }

    @Test
    void requirementsAllowF2pBulkRunsWithoutReshapingInventory() {
        assertEquals(NamedLocation.VARROCK_SQUARE_FOUNTAIN, test.requirements().getLocation());
        assertEquals(InventoryPolicy.NO_CHANGE, test.requirements().resolveInventoryPolicy());
        assertFalse(test.requirements().isDestructive());
        assertTrue(test.requirements().getSideEffects().contains(SideEffect.MOVES_PLAYER));
        assertTrue(test.requirements().getTimeoutMs() >= 300_000);
    }

    private int[][][] instanceChunks() {
        int[][][] chunks = new int[4][13][13];
        for (int[][] plane : chunks) for (int[] row : plane) Arrays.fill(row, -1);
        when(view.isInstance()).thenReturn(true);
        when(scene.isInstance()).thenReturn(true);
        when(view.getInstanceTemplateChunks()).thenReturn(chunks);
        when(scene.getInstanceTemplateChunks()).thenReturn(chunks);
        return chunks;
    }
}
