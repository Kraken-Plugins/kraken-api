package com.kraken.api.simulation.colosseum.engine;

import com.kraken.api.simulation.oracle.colosim.Simulation;
import com.kraken.api.simulation.colosseum.ColosseumPack;
import com.kraken.api.simulation.colosseum.TestArenas;
import com.kraken.api.simulation.data.MechanicsData;
import com.kraken.api.simulation.player.Command;
import com.kraken.api.simulation.geom.Coords;
import com.kraken.api.simulation.world.Frame;
import com.kraken.api.simulation.geom.Grid;
import com.kraken.api.simulation.world.Scratch;
import com.kraken.api.simulation.world.SimState;
import com.kraken.api.simulation.tick.Engine;
import com.kraken.api.simulation.geom.Los;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Route-finding NPC movement (no colosim oracle — colosim has no smart pathers):
 * warbanders and the Red Flag minotaur must walk around pillars to melee reach in
 * flood-fill-optimal time, degrade to single file behind each other, and reduce exactly
 * to the naive step on open ground. The naive control stalling on the pillar is asserted
 * alongside so the contrast that motivates smart pathing stays visible.
 */
public class SmartPathTest {
    private final ColosseumPack pack = ColosseumPack.load();
    private final MechanicsData data = pack.data();

    private Grid arena() {
        return TestArenas.grid(new Simulation());
    }

    private static Engine engineFor(Frame f) {
        Engine e = new Engine();
        e.beginPass();
        return e;
    }

    @Test
    public void warbandRoutesAroundPillarInFieldOptimalTime() {
        Grid g = arena();
        Frame f = TestArenas.frame(g, pack);
        SimState st = new SimState();
        st.frame = f;
        st.playerPos = Coords.pack(5, 9);
        // Pillar spans x 8-10, y 8-10; the warbander starts due east of it.
        TestArenas.addNpc(f, st, 0, data, "warband_berserker", 12, 9);
        f.finishOrder();
        st.rebuildOccupancy();
        Engine e = engineFor(f);
        int fieldDist = e.scratch().approachField(g, 1, 5, 9)[Coords.pack(12, 9) & 0xfff];
        assertTrue("scenario must require a detour", fieldDist > 7);
        // Runs 2 tiles per tick: reach falls on ceil(fieldDist / 2).
        int arrival = (fieldDist + 1) / 2;
        for (int t = 0; t < arrival; t++) {
            assertFalse("not adjacent before the field distance is run",
                    Los.meleeAdjacent(Coords.x(st.npcPos[0]), Coords.y(st.npcPos[0]), 1, 5, 9));
            e.advanceInPlace(st, Command.NONE);
            int x = Coords.x(st.npcPos[0]);
            int y = Coords.y(st.npcPos[0]);
            assertFalse("never inside a blocker", g.illegalStatic(1, x, y));
        }
        assertTrue("melee reach in exactly ceil(flood-fill distance / run speed)",
                Los.meleeAdjacent(Coords.x(st.npcPos[0]), Coords.y(st.npcPos[0]), 1, 5, 9));
    }

    @Test
    public void naiveMinotaurStallsAtPillarButRedFlagRoutesAround() {
        for (boolean redFlag : new boolean[]{false, true}) {
            Grid g = arena();
            Frame f = TestArenas.frame(g, pack);
            SimState st = new SimState();
            st.frame = f;
            st.playerPos = Coords.pack(5, 9);
            TestArenas.addNpc(f, st, 0, data, "minotaur", 12, 8);
            if (redFlag) {
                f.setSmartPath(0);
            }
            f.finishOrder();
            st.rebuildOccupancy();
            Engine e = engineFor(f);
            boolean adjacent = false;
            for (int t = 0; t < 40 && !adjacent; t++) {
                e.advanceInPlace(st, Command.NONE);
                adjacent = Los.meleeAdjacent(Coords.x(st.npcPos[0]), Coords.y(st.npcPos[0]), 3, 5, 9);
            }
            assertEquals(redFlag ? "Red Flag minotaur must route to melee reach"
                    : "naive minotaur must stay pillar-blocked (safespot works)", redFlag, adjacent);
        }
    }

    @Test
    public void smartRunsToReachInFieldOptimalTimeOnOpenGround() {
        // Axis-first ties mean the smart route is a dog-leg rather than the naive
        // diagonal, but open ground is still covered in ceil(fieldDist / 2) ticks.
        Grid g = arena();
        Frame f = TestArenas.frame(g, pack);
        SimState st = new SimState();
        st.frame = f;
        st.playerPos = Coords.pack(14, 16);
        TestArenas.addNpc(f, st, 0, data, "warband_berserker", 20, 28);
        f.finishOrder();
        st.rebuildOccupancy();
        Engine e = engineFor(f);
        int fieldDist = e.scratch().approachField(g, 1, 14, 16)[Coords.pack(20, 28) & 0xfff];
        int arrival = (fieldDist + 1) / 2;
        for (int t = 0; t < arrival; t++) {
            assertFalse("not adjacent before the field distance is run",
                    Los.meleeAdjacent(Coords.x(st.npcPos[0]), Coords.y(st.npcPos[0]), 1, 14, 16));
            e.advanceInPlace(st, Command.NONE);
            assertFalse("never inside a blocker",
                    g.illegalStatic(1, Coords.x(st.npcPos[0]), Coords.y(st.npcPos[0])));
        }
        assertTrue("melee reach in exactly ceil(flood-fill distance / run speed)",
                Los.meleeAdjacent(Coords.x(st.npcPos[0]), Coords.y(st.npcPos[0]), 1, 14, 16));
    }

    @Test
    public void twoWarbandersFormSingleFile() {
        Grid g = arena();
        Frame f = TestArenas.frame(g, pack);
        SimState st = new SimState();
        st.frame = f;
        st.playerPos = Coords.pack(5, 9);
        TestArenas.addNpc(f, st, 0, data, "warband_berserker", 12, 9);
        TestArenas.addNpc(f, st, 1, data, "warband_archer", 13, 9);
        f.finishOrder();
        st.rebuildOccupancy();
        Engine e = engineFor(f);
        for (int t = 0; t < 30; t++) {
            e.advanceInPlace(st, Command.NONE);
            assertFalse("no overlap while filing through",
                    st.npcPos[0] == st.npcPos[1]);
        }
        assertTrue(Los.meleeAdjacent(Coords.x(st.npcPos[0]), Coords.y(st.npcPos[0]), 1, 5, 9));
        // The strict-descent follower parks one field step behind the ring when the
        // leader claims the shared corridor's reach cell (A19: waits rather than
        // contouring around a dynamic blocker).
        short[] field = e.scratch().approachField(g, 1, 5, 9);
        assertTrue("follower files in directly behind the ring",
                field[st.npcPos[1] & 0xfff] <= 1);
    }

    @Test
    public void approachFieldIsCachedWithinPass() {
        Grid g = arena();
        Scratch sc = new Scratch();
        sc.beginPass();
        short[] a = sc.approachField(g, 1, 10, 16);
        short[] b = sc.approachField(g, 1, 10, 16);
        assertTrue("same array instance while player position is unchanged", a == b);
        assertEquals(0, a[Coords.pack(11, 16) & 0xfff]);
        assertEquals(0, a[Coords.pack(10, 15) & 0xfff]);
        assertEquals(1, a[Coords.pack(11, 17) & 0xfff]);
        assertEquals(Scratch.UNREACHED, a[Coords.pack(9, 9) & 0xfff]);
    }
}
