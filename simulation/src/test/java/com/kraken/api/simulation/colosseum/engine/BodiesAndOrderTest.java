package com.kraken.api.simulation.colosseum.engine;

import com.kraken.api.simulation.colosseum.ColosseumPack;
import com.kraken.api.simulation.colosseum.ScenarioFixture;
import com.kraken.api.simulation.hits.Hits;
import com.kraken.api.simulation.hits.Style;
import com.kraken.api.simulation.data.MechanicsData;
import com.kraken.api.simulation.player.Command;
import com.kraken.api.simulation.geom.Coords;
import com.kraken.api.simulation.world.Frame;
import com.kraken.api.simulation.world.SimState;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Movement rules settled against the seven live traces on 2026-11-26: NPCs take turns in NPC
 * index order; a body blocks other NPCs until it is deleted on its own turn 4 ticks after the
 * kill (5 for the large ones); the warband runs through other NPCs and other NPCs walk
 * through it; an attack click on a target out of reach walks the player in.
 */
public class BodiesAndOrderTest {
    @Test
    public void turnsGoInNpcIndexOrderWithEngineSpawnsLast() {
        MechanicsData data = ColosseumPack.load().data();
        Frame f = new Frame();
        f.data = data;
        int shaman = data.typeIndex("serpent_shaman");
        int[] rlIndex = {50, 10, -1, 30};
        for (int i = 0; i < rlIndex.length; i++) {
            f.npcTypeIdx[i] = (byte) shaman;
            f.npcIndex[i] = rlIndex[i];
        }
        f.finishOrder();
        assertEquals(4, f.npcOrderCount);
        assertArrayEquals(new byte[]{1, 3, 0, 2}, java.util.Arrays.copyOf(f.npcOrder, 4));
    }

    /** Player west-melees a shaman on (14,16); a jaguar on (12,16) wants (13,16), which
     *  overlaps the shaman's tile. The melee hit lands on tick 1. */
    private ScenarioFixture bodyInTheWay(int shamanSlot, int jaguarSlot) {
        ScenarioFixture fx = new ScenarioFixture();
        fx.player(15, 16);
        fx.npc(shamanSlot, "serpent_shaman", 14, 16);
        fx.npc(jaguarSlot, "jaguar_warrior", 12, 16);
        fx.equip(0, Style.MELEE, 4, 1, 200);
        fx.finish();
        fx.tick(Command.attack(shamanSlot) | Command.overhead(Command.OVERHEAD_MAGIC));
        return fx;
    }

    @Test
    public void aBodyBlocksUntilItIsDeletedOnItsTurnFourTicksAfterTheKill() {
        ScenarioFixture fx = bodyInTheWay(0, 1);
        fx.ticks(1);
        assertFalse("killed on tick 1", fx.st.alive(0));
        assertTrue(fx.st.corpse(0));
        fx.ticks(3);
        assertEquals("blocked through tick 4", Coords.pack(12, 16), fx.st.npcPos[1]);
        fx.ticks(1);
        assertFalse("deleted on tick 5", fx.st.corpse(0));
        assertEquals("a later index steps in on the deletion tick", Coords.pack(13, 16), fx.st.npcPos[1]);
    }

    @Test
    public void aMoverEarlierInTheTurnOrderStepsInATickLater() {
        ScenarioFixture fx = bodyInTheWay(1, 0);
        fx.ticks(4);
        fx.ticks(1);
        assertEquals("its turn came before the deletion on tick 5", Coords.pack(12, 16), fx.st.npcPos[0]);
        fx.ticks(1);
        assertEquals(Coords.pack(13, 16), fx.st.npcPos[0]);
    }

    @Test
    public void otherNpcsWalkThroughTheWarband() {
        ScenarioFixture fx = new ScenarioFixture();
        fx.player(16, 16);
        fx.npc(0, "warband_archer", 15, 16);
        fx.st.npcSpawnTick[0] = 0;
        fx.npc(1, "jaguar_warrior", 13, 16);
        fx.finish();
        assertEquals("the warband is never on the occupancy board", 0L, fx.st.occupancy[16] & 1L << 15);
        fx.ticks(1);
        assertEquals("the jaguar's step onto the archer's tile is not blocked", Coords.pack(14, 16), fx.st.npcPos[1]);
        assertEquals(Coords.pack(15, 16), fx.st.npcPos[0]);
    }

    @Test
    public void theWarbandRunsThroughOtherNpcs() {
        ScenarioFixture fx = new ScenarioFixture();
        fx.player(16, 16);
        fx.npc(0, "warband_archer", 12, 16);
        fx.st.npcSpawnTick[0] = 0;
        fx.npc(1, "serpent_shaman", 14, 16);
        fx.finish();
        fx.ticks(3);
        assertEquals("ran 2 a tick from tick 2, through the shaman", Coords.pack(14, 16), fx.st.npcPos[0]);
        fx.ticks(1);
        assertEquals("on its west side", Coords.pack(15, 16), fx.st.npcPos[0]);
        assertEquals(Coords.pack(14, 16), fx.st.npcPos[1]);
    }

    @Test
    public void anAttackClickOutOfReachWalksInAndAttacksOnArrival() {
        ScenarioFixture fx = new ScenarioFixture();
        fx.player(16, 16);
        fx.npc(0, "serpent_shaman", 21, 16);
        fx.equip(0, Style.MELEE, 4, 1, 30);
        fx.finish();
        fx.tick(Command.attack(0) | Command.overhead(Command.OVERHEAD_MAGIC));
        assertEquals("ran two tiles toward it", Coords.pack(18, 16), fx.st.playerPos);
        assertEquals("no attack from out of reach", 0, vsNpcHits(fx.st));
        fx.tick(Command.NONE);
        assertEquals("on the tile beside it", Coords.pack(20, 16), fx.st.playerPos);
        assertEquals("attacked on the arrival tick", 1, vsNpcHits(fx.st));
    }

    private static int vsNpcHits(SimState st) {
        int n = 0;
        for (int i = 0; i < st.pendingCount; i++) {
            if (Hits.isVsNpc(st.pendingHits[i])) {
                n++;
            }
        }
        return n;
    }
}
