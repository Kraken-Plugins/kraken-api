package com.kraken.api.simulation.live;

import com.kraken.api.simulation.data.MechanicsData;
import com.kraken.api.simulation.data.NpcDef;
import com.kraken.api.simulation.data.PipelineFlags;
import com.kraken.api.simulation.data.PlayerData;
import com.kraken.api.simulation.geom.Grid;
import com.kraken.api.simulation.hits.Style;
import net.runelite.api.Client;
import net.runelite.api.CollisionData;
import net.runelite.api.CollisionDataFlag;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Prayer;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The live layer against a mocked client: collision capture, server tiles and animation stamping, on a plain (not
 * instanced) scene whose base sits 16 tiles south west of the anchored region's base.
 */
class LiveCaptureTest {
    private static final int REGION_X = 3200;
    private static final int REGION_Y = 3200;
    private static final int SCENE_BASE_X = REGION_X - 16;
    private static final int SCENE_BASE_Y = REGION_Y - 16;
    private static final int SIZE = 104;

    private final Client client = mock(Client.class);
    private final WorldView wv = mock(WorldView.class);
    private final RegionAnchor anchor = new RegionAnchor();

    @BeforeEach
    void setUp() {
        when(client.getTopLevelWorldView()).thenReturn(wv);
        when(client.getWorldView(anyInt())).thenReturn(wv);
        when(wv.isInstance()).thenReturn(false);
        when(wv.getBaseX()).thenReturn(SCENE_BASE_X);
        when(wv.getBaseY()).thenReturn(SCENE_BASE_Y);
        when(wv.getPlane()).thenReturn(0);
        when(wv.getSizeX()).thenReturn(SIZE);
        when(wv.getSizeY()).thenReturn(SIZE);
        assertTrue(anchor.anchor(new WorldPoint(REGION_X + 5, REGION_Y + 5, 0)));
    }

    @Test
    void sceneFlagsBecomeTheWindowsBoards() {
        int[][] flags = new int[SIZE][SIZE];
        // Region local (3, 4) is scene (19, 20): a movement and sight blocker; (6, 4) blocks movement only.
        flags[19][20] = CollisionDataFlag.BLOCK_MOVEMENT_FULL | CollisionDataFlag.BLOCK_LINE_OF_SIGHT_FULL;
        flags[22][20] = CollisionDataFlag.BLOCK_MOVEMENT_FULL;
        CollisionData collision = mock(CollisionData.class);
        when(collision.getFlags()).thenReturn(flags);
        when(wv.getCollisionMaps()).thenReturn(new CollisionData[]{collision, null, null, null});

        Grid grid = CollisionCapture.capture(client, anchor);
        assertNotNull(grid);
        assertTrue(grid.blockedMove(3, 4));
        assertTrue(grid.blockedLos(3, 4));
        assertTrue(grid.blockedMove(6, 4));
        assertFalse(grid.blockedLos(6, 4));
        assertFalse(grid.blockedMove(5, 4));
        assertTrue(grid.illegalStatic(2, 2, 3), "the eroded masks are finished");
        // The scene covers region local 0..87; everything is observed, so nothing inside stays blocked by default.
        assertFalse(grid.blockedMove(63, 63));
    }

    @Test
    void tilesOutsideTheSceneStayBlocked() {
        int[][] flags = new int[SIZE][SIZE];
        Grid grid = CollisionCapture.fromFlags(flags, 10, 10, (sx, sy) -> sx | sy << 6);
        assertFalse(grid.blockedMove(9, 9));
        assertTrue(grid.blockedMove(10, 10), "never observed: blocked, the safe default");
        assertTrue(grid.blockedLos(30, 2));
    }

    @Test
    void missingCollisionMapsCaptureNothing() {
        when(wv.getCollisionMaps()).thenReturn(null);
        assertEquals(null, CollisionCapture.capture(client, anchor));
    }

    @Test
    void serverTileReadsTheWorldLocationBackToATemplatePoint() {
        NPC npc = mock(NPC.class);
        WorldPoint server = new WorldPoint(REGION_X + 7, REGION_Y + 9, 0);
        when(npc.getWorldLocation()).thenReturn(server);
        assertEquals(server, LiveTiles.serverTile(client, npc));
        assertEquals(7 | 9 << 6, anchor.local(LiveTiles.serverTile(client, npc), 0));
    }

    private static MechanicsData tables() {
        NpcDef brute = new NpcDef();
        brute.key = "brute";
        brute.ids = new int[]{900};
        brute.size = 2;
        brute.style = "magic";
        brute.hitDelay = "projectile";
        brute.projectileBase = 30;
        brute.projectilePerTile = 30;
        brute.attackAnims = new int[]{4000, 4001};
        brute.defendAnims = new int[]{4002};
        PipelineFlags flags = new PipelineFlags();
        flags.attackAnimationLagTicks = 1;
        return new MechanicsData(new NpcDef[]{brute}, new PlayerData(), flags);
    }

    @Test
    void foldStampsAttacksAndSkipsDefendsDeathsAndTheUnslotted() {
        MechanicsData data = tables();
        AnimationStamper stamper = new AnimationStamper(data);
        NpcIndexer indexer = new NpcIndexer();
        CooldownTracker cooldowns = new CooldownTracker();
        assertEquals(0, indexer.slotForOrAssign(11));
        assertEquals(1, indexer.slotForOrAssign(12));
        int npcTile = 10 | 10 << 6;
        int playerTile = 14 | 10 << 6;
        stamper.npcAnimation(11, 900, 4000, npcTile, playerTile, false);
        stamper.npcAnimation(12, 900, 4002, npcTile, playerTile, false);
        stamper.npcAnimation(13, 900, 4000, npcTile, playerTile, false);
        stamper.playerAnimation(7000);
        List<String> heard = new ArrayList<>();
        stamper.fold(100, Collections.emptySet(), indexer, cooldowns, new AnimationStamper.Hook() {
            @Override
            public void onLaunch(int npcIndex, int slot, NpcDef def, int animId, int tile, int tick) {
                heard.add(npcIndex + ":" + slot + ":" + animId + ":" + tile);
            }

            @Override
            public void onPlayerAttack(int animId, int tick) {
                heard.add("player:" + animId);
            }
        });
        assertEquals(List.of("11:0:4000:" + playerTile, "player:7000"), heard);
        assertEquals(99, cooldowns.lastLaunch(0), "stamped one tick early under the lag");
        // Size 2 at (10, 10) to the player at (14, 10): distance 3, so (max(0, 30 + 90) + 0) / 30 = 4 ticks.
        assertEquals(3, cooldowns.inFlightRemaining(0, 100));
        assertEquals(Style.MAGIC, cooldowns.inFlightStyle(0));
        assertEquals(Integer.MIN_VALUE, cooldowns.lastLaunch(1), "a defend animation never stamps");
        assertEquals(3, cooldowns.playerCooldownAt(4, 100));

        stamper.npcAnimation(11, 900, 4001, npcTile, playerTile, true);
        stamper.fold(110, Collections.singleton(11), indexer, cooldowns, new AnimationStamper.Hook() {
        });
        assertEquals(99, cooldowns.lastLaunch(0), "an NPC that died this tick does not stamp");
    }

    @Test
    void theHookDecidesStampingAndPrayerIgnoringLaunches() {
        MechanicsData data = tables();
        AnimationStamper stamper = new AnimationStamper(data);
        NpcIndexer indexer = new NpcIndexer();
        CooldownTracker cooldowns = new CooldownTracker();
        indexer.slotForOrAssign(11);
        indexer.slotForOrAssign(12);
        stamper.npcAnimation(11, 900, 4000, 0, 3, false);
        stamper.npcAnimation(12, 900, 4001, 0, 3, false);
        stamper.fold(50, Collections.emptySet(), indexer, cooldowns, new AnimationStamper.Hook() {
            @Override
            public boolean stampsCooldown(NpcDef def, int slot, int animId, int tick) {
                return animId != 4000;
            }

            @Override
            public boolean ignoresPrayer(NpcDef def, int animId) {
                return animId == 4001;
            }
        });
        assertEquals(Integer.MIN_VALUE, cooldowns.lastLaunch(0), "a charge animation the pack keeps for itself");
        assertEquals(49, cooldowns.lastLaunch(1));
        assertTrue(cooldowns.inFlightBlocked(1), "a prayer ignoring launch is stamped blocked");
    }

    @Test
    void observeBuffersTheServerTilesAndThePrayerAtLaunch() {
        MechanicsData data = tables();
        AnimationStamper stamper = new AnimationStamper(data);
        Player local = mock(Player.class);
        when(client.getLocalPlayer()).thenReturn(local);
        when(local.getWorldLocation()).thenReturn(new WorldPoint(REGION_X + 14, REGION_Y + 10, 0));
        NPC npc = mock(NPC.class);
        when(npc.getId()).thenReturn(900);
        when(npc.getIndex()).thenReturn(11);
        when(npc.getAnimation()).thenReturn(4000);
        when(npc.getWorldLocation()).thenReturn(new WorldPoint(REGION_X + 10, REGION_Y + 10, 0));
        when(client.getVarbitValue(Prayer.PROTECT_FROM_MAGIC.getVarbit())).thenReturn(1);

        stamper.observe(client, npc, anchor);
        NpcIndexer indexer = new NpcIndexer();
        indexer.slotForOrAssign(11);
        CooldownTracker cooldowns = new CooldownTracker();
        int[] tile = new int[1];
        stamper.fold(100, Collections.emptySet(), indexer, cooldowns, new AnimationStamper.Hook() {
            @Override
            public void onLaunch(int npcIndex, int slot, NpcDef def, int animId, int playerTile, int tick) {
                tile[0] = playerTile;
            }
        });
        assertEquals(14 | 10 << 6, tile[0]);
        assertTrue(cooldowns.inFlightBlocked(0), "Protect from Magic was up at launch");

        when(npc.isDead()).thenReturn(true);
        stamper.observe(client, npc, anchor);
        stamper.fold(101, Collections.emptySet(), indexer, cooldowns, new AnimationStamper.Hook() {
        });
        assertEquals(99, cooldowns.lastLaunch(0), "a dead NPC's animation is never buffered");
    }
}
