package com.kraken.api.simulation.colosseum.combat;

import com.kraken.api.simulation.colosseum.ScenarioFixture;
import com.kraken.api.simulation.data.NpcDef;
import com.kraken.api.simulation.geom.Footprints;
import com.kraken.api.simulation.geom.Los;
import com.kraken.api.simulation.hits.Style;
import com.kraken.api.simulation.world.Frame;
import com.kraken.api.simulation.world.SimState;
import com.kraken.api.simulation.player.Command;
import com.kraken.api.simulation.geom.Coords;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Melee-pull accounting: the target term accrues, per tick, the tiles left to travel
 * until melee adjacency with a melee-best target (clamped at 15); the pack-pull term
 * rewards closeness (15 minus that distance) over every alive wave-holding melee-best
 * NPC, but only on ticks where no ranged attacker has LOS to the player. Ranged-best
 * NPCs feed neither.
 */
public class MeleeDistanceTest {
    @Test
    public void accumulatesClampedDistanceForMeleeBestTarget() {
        ScenarioFixture fx = new ScenarioFixture();
        fx.player(10, 16);
        // The tribrid fixture makes melee the archer's best set (style triangle 50 dmg).
        fx.npc(0, "warband_archer", 18, 16);
        fx.equipDefault();
        fx.st.gearSet = 2;
        fx.finish();
        observe(fx);

        int expectedDist = 0;
        int expectedPull = 0;
        fx.tick(Command.attack(0));
        expectedDist += distance(fx, 0);
        expectedPull += closeness(fx, 0);
        for (int i = 0; i < 11; i++) {
            fx.tick(Command.NONE);
            expectedDist += distance(fx, 0);
            expectedPull += closeness(fx, 0);
        }
        assertTrue("the smart-pathing archer must have closed to melee",
                chebyshev(fx, 0) <= 1 || !fx.st.alive(0));
        assertEquals(expectedDist, dist(fx));
        assertTrue("distance was accrued while apart", dist(fx) > 0);
        assertEquals("no ranged attacker on the board: every tick is covered",
                expectedPull, pull(fx));

        int atContact = dist(fx);
        fx.ticks(3);
        assertEquals("adjacency accrues no target distance", atContact, dist(fx));
    }

    @Test
    public void packPullCountsEveryMeleeBestNpcWhileCovered() {
        ScenarioFixture fx = new ScenarioFixture();
        fx.player(10, 16);
        fx.npc(0, "warband_archer", 18, 16);
        fx.npc(1, "warband_archer", 14, 20);
        fx.equipDefault();
        fx.st.gearSet = 2;
        fx.finish();
        observe(fx);

        int expectedTarget = 0;
        int expectedPull = 0;
        fx.tick(Command.attack(0));
        expectedTarget += distance(fx, 0);
        expectedPull += closeness(fx, 0) + closeness(fx, 1);
        for (int i = 0; i < 9; i++) {
            fx.tick(Command.NONE);
            expectedTarget += distance(fx, 0);
            expectedPull += closeness(fx, 0) + closeness(fx, 1);
        }
        assertEquals(expectedTarget, dist(fx));
        assertEquals("both archers feed the pull", expectedPull, pull(fx));
        assertTrue(pull(fx) > 0);
    }

    @Test
    public void noPullCreditWhileARangedAttackerHasSight() {
        ScenarioFixture fx = new ScenarioFixture();
        fx.player(10, 16);
        fx.npc(0, "warband_archer", 18, 16);
        fx.npc(1, "serpent_shaman", 18, 12);
        fx.equipDefault();
        fx.st.gearSet = 2;
        fx.finish();
        observe(fx);
        assertTrue("the shaman must start with sight and range",
                Los.npcCanAttack(fx.frame.grid, 18, 12, 1, 10, 10, 16));

        fx.tick(Command.attack(0));
        fx.ticks(7);
        assertTrue("target distance still accrues while exposed",
                dist(fx) > 0);
        assertEquals("exposed ticks earn no pack pull", 0, pull(fx));
    }

    @Test
    public void noAccumulationForRangedBestTarget() {
        ScenarioFixture fx = new ScenarioFixture();
        fx.player(10, 16);
        fx.npc(0, "serpent_shaman", 18, 16);
        fx.equipDefault();
        fx.finish();
        observe(fx);

        fx.tick(Command.attack(0));
        fx.ticks(5);
        assertEquals("ranged-best targets never feed the melee integral",
                0, dist(fx));
        assertEquals(0, pull(fx));
    }

    private static final int LANE_PULL = 4;
    private static final int LANE_DIST = 5;

    private static int pull(ScenarioFixture fx) {
        return fx.st.accLane[LANE_PULL];
    }

    private static int dist(ScenarioFixture fx) {
        return fx.st.accLane[LANE_DIST];
    }

    /** Installs the planner measures V2 kept in the engine: pack pull and melee target distance, written into
     *  accLane 4 and 5 once per tick. */
    private static void observe(ScenarioFixture fx) {
        fx.engine.tickObserver = MeleeDistanceTest::measure;
    }

    private static void measure(SimState st) {
        Frame f = st.frame;
        byte[] meleeBest = f.bestSetForType;
        if (meleeBest == null) {
            return;
        }
        int ts = st.targetSlot;
        boolean covered = st.tickLosExposure == 0;
        for (int oi = 0; oi < f.npcOrderCount; oi++) {
            int i = f.npcOrder[oi];
            if (!st.alive(i)) {
                continue;
            }
            NpcDef def = f.def(i);
            if (!def.waveClearing) {
                continue;
            }
            int bs = meleeBest[f.npcTypeIdx[i]];
            if (bs < 0 || f.equipSets[bs].styleCode != Style.MELEE) {
                continue;
            }
            int d = Footprints.distance(Coords.x(st.npcPos[i]), Coords.y(st.npcPos[i]),
                    def.size, Coords.x(st.playerPos), Coords.y(st.playerPos), 1) - 1;
            int clamped = Math.min(Math.max(d, 0), 15);
            if (covered) {
                st.accLane[LANE_PULL] += 15 - clamped;
            }
            if (i == ts) {
                st.accLane[LANE_DIST] += clamped;
            }
        }
    }

    /** Mirrors the engine hook: clamp(gap - 1, 0, 15), zero once the NPC is dead. */
    private static int distance(ScenarioFixture fx, int slot) {
        if (!fx.st.alive(slot)) {
            return 0;
        }
        return Math.min(Math.max(chebyshev(fx, slot) - 1, 0), 15);
    }

    private static int closeness(ScenarioFixture fx, int slot) {
        if (!fx.st.alive(slot)) {
            return 0;
        }
        return 15 - distance(fx, slot);
    }

    private static int chebyshev(ScenarioFixture fx, int slot) {
        int dx = Math.abs(Coords.x(fx.st.npcPos[slot]) - Coords.x(fx.st.playerPos));
        int dy = Math.abs(Coords.y(fx.st.npcPos[slot]) - Coords.y(fx.st.playerPos));
        return Math.max(dx, dy);
    }
}
