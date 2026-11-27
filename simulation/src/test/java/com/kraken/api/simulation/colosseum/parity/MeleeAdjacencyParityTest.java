package com.kraken.api.simulation.colosseum.parity;

import com.kraken.api.simulation.oracle.colosim.Simulation;
import com.kraken.api.simulation.colosseum.TestArenas;
import com.kraken.api.simulation.geom.Los;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Melee reach: standard melee NPCs attack from tiles cardinally against their footprint
 * only — never corners. The pure adjacency formula is checked against the oracle's
 * range-1 result on open ground (an unblocked patch away from pillars), plus directed
 * corner and edge cases per footprint size.
 */
public class MeleeAdjacencyParityTest {
    @Test
    public void adjacencyMatchesOracleOnOpenGround() {
        Simulation sim = new Simulation();
        // Open patch around colosim (16, 16): mid-arena, clear of pillars and walls.
        for (int size = 1; size <= 3; size++) {
            int nx = 15;
            int ncy = 16;
            for (int px = nx - 3; px <= nx + size + 2; px++) {
                for (int pcy = ncy - size - 2; pcy <= ncy + 3; pcy++) {
                    boolean expect = sim.hasLOS(nx, ncy, px, pcy, size, 1, true);
                    boolean got = Los.meleeAdjacent(nx, TestArenas.flip(ncy), size, px, TestArenas.flip(pcy))
                            && !(px >= nx && px < nx + size && pcy <= ncy && pcy > ncy - size);
                    assertEquals("size " + size + " npc(" + nx + "," + ncy + ") player(" + px + "," + pcy + ")",
                            expect, got);
                }
            }
        }
    }

    @Test
    public void cornersNeverAdjacent() {
        // Engine coordinates: size-2 footprint anchored (10,10) spans (10..11, 10..11).
        assertFalse(Los.meleeAdjacent(10, 10, 2, 9, 9));
        assertFalse(Los.meleeAdjacent(10, 10, 2, 12, 12));
        assertFalse(Los.meleeAdjacent(10, 10, 2, 9, 12));
        assertFalse(Los.meleeAdjacent(10, 10, 2, 12, 9));
        assertTrue(Los.meleeAdjacent(10, 10, 2, 10, 9));
        assertTrue(Los.meleeAdjacent(10, 10, 2, 11, 12));
        assertTrue(Los.meleeAdjacent(10, 10, 2, 9, 10));
        assertTrue(Los.meleeAdjacent(10, 10, 2, 12, 11));
    }

    @Test
    public void insideFootprintNotAdjacent() {
        assertFalse(Los.meleeAdjacent(10, 10, 3, 11, 11));
        assertFalse(Los.meleeAdjacent(10, 10, 1, 10, 10));
    }
}
