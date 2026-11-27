package com.kraken.api.simulation.colosseum;

import com.kraken.api.simulation.geom.Coords;
import com.kraken.api.simulation.oracle.colosim.Simulation;
import com.kraken.api.simulation.tick.Engine;
import com.kraken.api.simulation.world.Frame;
import com.kraken.api.simulation.world.SimState;

/**
 * Auto Colosseum V2's throughput board, shared by the throughput gate and the JMH benchmarks: a busy wave 8 board
 * (seed 7, player at 33, 28, no modifiers, the tribrid loadout) and the horizon 10 rollout V2 times on it. V2 drives
 * the rollout with its private reflex policy; on this board that policy issues the same ten commands every rollout,
 * recorded in {@link #REFLEX_COMMANDS}, so the rollout here measures the engine alone.
 */
public final class ThroughputBoard {
    /** V2 3.0.0's reflex commands on this board (2026-11-26): a move click to (40, 25), then prayer switches. */
    public static final long[] REFLEX_COMMANDS = {104040, 0, 0, 32768, 0, 0, 98304, 32768, 0, 0};

    private ThroughputBoard() {
    }

    /** Builds the board on the pack. */
    public static SimState build(ColosseumPack pack) {
        Frame frame = new Frame();
        SimState base = new SimState();
        ColosseumBoards.build(pack, TestArenas.liveGrid(new Simulation()), 8, Mods.NONE, 7L,
                Coords.pack(33, 28), frame, base);
        Loadouts.arm(pack.data(), frame, base);
        return base;
    }

    /** One horizon 10 rollout from the board into a working state. */
    public static void rollout(Engine engine, SimState base, SimState work) {
        work.copyFrom(base);
        work.resetAccumulators();
        for (long command : REFLEX_COMMANDS) {
            engine.advanceInPlace(work, command);
        }
    }
}
