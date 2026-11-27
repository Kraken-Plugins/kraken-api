package com.kraken.api.simulation.colosseum.trace;

import com.kraken.api.simulation.colosseum.ColosseumBoards;
import com.kraken.api.simulation.colosseum.ColosseumPack;
import com.kraken.api.simulation.colosseum.Loadouts;
import com.kraken.api.simulation.colosseum.TestArenas;
import com.kraken.api.simulation.geom.Coords;
import com.kraken.api.simulation.geom.Grid;
import com.kraken.api.simulation.oracle.colosim.Simulation;
import com.kraken.api.simulation.tick.Engine;
import com.kraken.api.simulation.trace.SimTraceRecorder;
import com.kraken.api.simulation.trace.Traces;
import com.kraken.api.simulation.trace.WaveTrace;
import com.kraken.api.simulation.trace.WaveTraceWriter;
import com.kraken.api.simulation.world.Frame;
import com.kraken.api.simulation.world.SimState;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntToLongFunction;

/**
 * Shared trace test material: a synthetic wave stepped under scripted commands with the sim recorder attached,
 * producing an in memory trace in the live comparable shape.
 */
final class TraceTestRuns {
    static final int PLAYER_START = Coords.pack(33, 28);

    final ColosseumPack pack = ColosseumPack.load();
    final Grid grid = TestArenas.liveGrid(new Simulation());
    final Frame frame = new Frame();
    final SimState state = new SimState();
    final List<String> lines = new ArrayList<>();

    /** Builds the wave, advances {@code ticks} times with {@code cmdAt} per advance, and serializes the whole run
     *  (initial record at t = -1, advances at t = 0..ticks-1). */
    TraceTestRuns run(int wave, long seed, int ticks, IntToLongFunction cmdAt) {
        ColosseumBoards.build(pack, grid, wave, pack.mods(0, 0), seed, PLAYER_START, frame, state);
        Loadouts.arm(pack.data(), frame, state);
        WaveTraceWriter writer = new WaveTraceWriter(lines::add, pack.trace());
        SimTraceRecorder rec = new SimTraceRecorder(writer);
        rec.begin(Traces.metaFrom(frame, "sim", pack.trace()), frame, state);
        Engine engine = new Engine();
        engine.attackListener = rec;
        engine.beginPass();
        for (int k = 0; k < ticks; k++) {
            engine.advanceInPlace(state, cmdAt.applyAsLong(k));
            rec.afterAdvance(frame, state);
        }
        rec.end("aborted", "test horizon");
        return this;
    }

    WaveTrace parse() throws IOException {
        return WaveTrace.read(new StringReader(String.join("\n", lines)));
    }
}
