package com.kraken.api.simulation.colosseum.parity;

import com.kraken.api.simulation.oracle.colosim.Simulation;
import com.kraken.api.simulation.colosseum.TestArenas;
import com.kraken.api.simulation.geom.Los;
import com.kraken.api.simulation.geom.Grid;
import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * Exhaustive range+sight parity against the colosim oracle over the whole arena: every
 * (npc anchor, player tile) pair in the colosim window, for the size/range combinations
 * that occur in waves 1-11 (melee 1x1/2x2/3x3, shaman 1x1 r10, colossi/manticore 3x3 r15,
 * plus 2x2 r15 for coverage). Blocked anchors and player-inside-footprint cases are
 * included deliberately — both simulators must agree on them too.
 */
public class LosParityTest {
    @Test
    public void exhaustiveNpcAttackParity() {
        Simulation sim = new Simulation();
        Grid g = TestArenas.grid(sim);
        int[][] combos = {{1, 1}, {2, 1}, {3, 1}, {1, 10}, {2, 15}, {3, 15}};
        int mismatches = 0;
        StringBuilder detail = new StringBuilder();
        for (int[] combo : combos) {
            int size = combo[0];
            int range = combo[1];
            for (int nx = 0; nx <= 34; nx++) {
                for (int ncy = 0; ncy <= TestArenas.MAX_CY; ncy++) {
                    for (int px = 0; px <= 34; px++) {
                        for (int pcy = 0; pcy <= TestArenas.MAX_CY; pcy++) {
                            boolean expect = sim.hasLOS(nx, ncy, px, pcy, size, range, true);
                            boolean got = Los.npcCanAttack(g, nx, TestArenas.flip(ncy), size, range,
                                    px, TestArenas.flip(pcy));
                            if (expect != got && ++mismatches <= 10) {
                                detail.append(String.format(
                                        "size %d range %d npc (%d,%d) player (%d,%d): oracle %s ours %s%n",
                                        size, range, nx, ncy, px, pcy, expect, got));
                            }
                        }
                    }
                }
            }
        }
        if (mismatches > 0) {
            fail(mismatches + " LOS mismatches, first:\n" + detail);
        }
    }
}
