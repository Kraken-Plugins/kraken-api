package com.kraken.api.simulation.colosseum;

import com.kraken.api.simulation.geom.Coords;
import com.kraken.api.simulation.tick.Engine;
import com.kraken.api.simulation.world.SimState;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The rollout throughput gate on Auto Colosseum V2's throughput board ({@link ThroughputBoard}): horizon 10 rollouts
 * must comfortably support a 2,000 rollouts per 15 ms search budget. Warmup, measurement and floor are V2's (the floor
 * is a third of the budget, so shared CI hardware cannot flake it); the honest measurement is the
 * {@code simulationBenchmark} task.
 */
public class ThroughputGateTest {
    private static final int MIN_ROLLOUTS_PER_15MS = 700;

    @Test
    public void rolloutsMeetTheSearchBudget() {
        ColosseumPack pack = ColosseumPack.load();
        SimState base = ThroughputBoard.build(pack);
        // V2's board, slot by slot (read from V2 3.0.0's SyntheticWaves on 2026-11-26): seven NPCs up, the
        // minotaur reinforcement pending in slot 7.
        String[] keys = {"warband_berserker", "warband_archer", "warband_seer", "javelin_colossus",
                "javelin_colossus", "manticore", "shockwave_colossus", "minotaur"};
        int[][] tiles = {{32, 39}, {31, 38}, {33, 38}, {40, 35}, {19, 32}, {44, 32}, {35, 37}};
        for (int slot = 0; slot < keys.length; slot++) {
            assertEquals("slot " + slot, keys[slot], base.frame.def(slot).key);
            if (slot < tiles.length) {
                assertEquals("slot " + slot + " tile", Coords.pack(tiles[slot][0], tiles[slot][1]), base.npcPos[slot]);
            }
        }
        assertEquals("the manticore's seeded pattern", 1, base.npcLane[Lanes.PATTERN][5]);
        SimState work = new SimState();
        Engine engine = new Engine();
        engine.beginPass();

        for (int warm = 0; warm < 4000; warm++) {
            ThroughputBoard.rollout(engine, base, work);
        }
        int measured = 4000;
        long start = System.nanoTime();
        for (int i = 0; i < measured; i++) {
            ThroughputBoard.rollout(engine, base, work);
        }
        long elapsed = System.nanoTime() - start;
        double per15ms = measured * 15_000_000.0 / elapsed;
        System.out.printf("ThroughputGateTest: %.0f rollouts per 15 ms%n", per15ms);
        assertTrue("rollout throughput " + Math.round(per15ms) + " per 15ms deadline (floor "
                + MIN_ROLLOUTS_PER_15MS + ", budget target 2000)", per15ms >= MIN_ROLLOUTS_PER_15MS);
    }
}
