package com.kraken.api.simulation.colosseum.engine;

import com.kraken.api.simulation.oracle.colosim.Simulation;
import com.kraken.api.simulation.colosseum.ColosseumPack;
import com.kraken.api.simulation.colosseum.TestArenas;
import com.kraken.api.simulation.player.Command;
import com.kraken.api.simulation.geom.Coords;
import com.kraken.api.simulation.world.Frame;
import com.kraken.api.simulation.geom.Grid;
import com.kraken.api.simulation.world.SimState;
import com.kraken.api.simulation.tick.Engine;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Player movement: OSRS breadth-first pathing (W,E,S,N,SW,SE,NW,NE expansion order,
 * size-1 corner rule), 1 tile walking / 2 running per tick, and the run-energy economy
 * (drain only on full running ticks, regeneration otherwise, auto-walk at empty).
 */
public class PlayerPathTest {
    private final ColosseumPack pack = ColosseumPack.load();

    private Frame openFrame() {
        Grid g = new Grid();
        g.finish();
        return TestArenas.frame(g, pack);
    }

    private SimState state(Frame f, int x, int y, boolean running) {
        SimState st = new SimState();
        st.frame = f;
        st.playerPos = Coords.pack(x, y);
        st.running = running;
        return st;
    }

    @Test
    public void walksOneTilePerTick() {
        Frame f = openFrame();
        SimState st = state(f, 5, 5, false);
        Engine e = new Engine();
        e.beginPass();
        e.advanceInPlace(st, Command.move(Coords.pack(10, 5)));
        for (int expectX = 6; expectX <= 10; expectX++) {
            assertEquals(expectX, Coords.x(st.playerPos));
            assertEquals(5, Coords.y(st.playerPos));
            if (expectX < 10) {
                e.advanceInPlace(st, Command.NONE);
            }
        }
        assertEquals("arrival clears the destination", Coords.NONE, st.moveDest);
    }

    @Test
    public void runsTwoTilesAndDrainsOnlyOnFullRunTicks() {
        Frame f = openFrame();
        SimState st = state(f, 5, 5, true);
        Engine e = new Engine();
        e.beginPass();
        // Weight 0, agility 99: drain (60+0)*(300-99)/300 = 40 per running tick,
        // regen 99/10+15 = 24 per non-running tick.
        e.advanceInPlace(st, Command.move(Coords.pack(10, 5)));
        assertEquals(7, Coords.x(st.playerPos));
        assertEquals(10000 - 40, st.runEnergy100);
        e.advanceInPlace(st, Command.NONE);
        assertEquals(9, Coords.x(st.playerPos));
        assertEquals(10000 - 80, st.runEnergy100);
        // One path tile left: consumed as a walking step, so the tick regenerates (A18).
        e.advanceInPlace(st, Command.NONE);
        assertEquals(10, Coords.x(st.playerPos));
        assertEquals(10000 - 80 + 24, st.runEnergy100);
        assertEquals(Coords.NONE, st.moveDest);
    }

    @Test
    public void walkingAndStandingRegenerate() {
        Frame f = openFrame();
        SimState st = state(f, 5, 5, false);
        st.runEnergy100 = 5000;
        Engine e = new Engine();
        e.beginPass();
        e.advanceInPlace(st, Command.NONE);
        assertEquals(5024, st.runEnergy100);
        e.advanceInPlace(st, Command.move(Coords.pack(6, 5)));
        assertEquals(5048, st.runEnergy100);
    }

    @Test
    public void emptyEnergyDropsToWalk() {
        Frame f = openFrame();
        SimState st = state(f, 5, 5, true);
        st.runEnergy100 = 30;
        Engine e = new Engine();
        e.beginPass();
        e.advanceInPlace(st, Command.move(Coords.pack(10, 5)));
        assertEquals("walked, not ran", 6, Coords.x(st.playerPos));
        assertFalse("run toggled off at empty energy", st.running);
    }

    @Test
    public void diagonalNeedsBothFlankingCardinals() {
        Grid g = new Grid();
        Grid.set(g.blockMove, 6, 5);
        g.finish();
        Frame f = TestArenas.frame(g, pack);
        SimState st = state(f, 5, 5, false);
        Engine e = new Engine();
        e.beginPass();
        e.advanceInPlace(st, Command.move(Coords.pack(6, 6)));
        assertEquals("blocked flank forces the cardinal-first detour", Coords.pack(5, 6), st.playerPos);
        e.advanceInPlace(st, Command.NONE);
        assertEquals(Coords.pack(6, 6), st.playerPos);
    }

    @Test
    public void tieBreakPrefersTheBfsExpansionOrder() {
        Frame f = openFrame();
        SimState st = state(f, 10, 10, false);
        Engine e = new Engine();
        e.beginPass();
        e.advanceInPlace(st, Command.move(Coords.pack(12, 12)));
        assertEquals("pure NE diagonal", Coords.pack(11, 11), st.playerPos);
        e.advanceInPlace(st, Command.NONE);
        assertEquals(Coords.pack(12, 12), st.playerPos);
    }

    @Test
    public void unreachableDestinationClearsWithoutMoving() {
        Grid g = new Grid();
        for (int x = 19; x <= 21; x++) {
            for (int y = 19; y <= 21; y++) {
                if (x != 20 || y != 20) {
                    Grid.set(g.blockMove, x, y);
                }
            }
        }
        g.finish();
        Frame f = TestArenas.frame(g, pack);
        SimState st = state(f, 5, 5, false);
        Engine e = new Engine();
        e.beginPass();
        e.advanceInPlace(st, Command.move(Coords.pack(20, 20)));
        assertEquals(Coords.pack(5, 5), st.playerPos);
        assertEquals(Coords.NONE, st.moveDest);
    }

    @Test
    public void newClickReroutesFromCurrentTile() {
        Frame f = openFrame();
        SimState st = state(f, 5, 5, false);
        Engine e = new Engine();
        e.beginPass();
        e.advanceInPlace(st, Command.move(Coords.pack(12, 5)));
        assertEquals(Coords.pack(6, 5), st.playerPos);
        e.advanceInPlace(st, Command.move(Coords.pack(6, 8)));
        assertEquals(Coords.pack(6, 6), st.playerPos);
        e.advanceInPlace(st, Command.NONE);
        e.advanceInPlace(st, Command.NONE);
        assertEquals(Coords.pack(6, 8), st.playerPos);
        assertEquals(Coords.NONE, st.moveDest);
    }

    @Test
    public void detoursAroundArenaPillar() {
        Simulation sim = new Simulation();
        Grid g = TestArenas.grid(sim);
        Frame f = TestArenas.frame(g, pack);
        // Pillar occupies x 8-10, y 8-10 (engine coords); walk straight across it.
        SimState st = state(f, 7, 9, false);
        Engine e = new Engine();
        e.beginPass();
        long cmd = Command.move(Coords.pack(12, 9));
        int prev = st.playerPos;
        int ticks = 0;
        e.advanceInPlace(st, cmd);
        while (st.moveDest != Coords.NONE && ticks < 20) {
            int cur = st.playerPos;
            assertFalse("never on a blocked tile", g.blockedMove(Coords.x(cur), Coords.y(cur)));
            assertTrue("steps stay adjacent", Math.abs(Coords.x(cur) - Coords.x(prev)) <= 1
                    && Math.abs(Coords.y(cur) - Coords.y(prev)) <= 1);
            prev = cur;
            e.advanceInPlace(st, Command.NONE);
            ticks++;
        }
        assertEquals(Coords.pack(12, 9), st.playerPos);
        // Straight-line distance is 5, but the corner rule forbids the diagonals that hug
        // the pillar corners, so the shortest legal route is 8 steps.
        assertEquals("shortest detour around the 3x3 pillar", 8, ticks + 1);
    }
}
