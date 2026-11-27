package com.kraken.api.simulation.bench;

import com.kraken.api.simulation.colosseum.ColosseumPack;
import com.kraken.api.simulation.colosseum.ThroughputBoard;
import com.kraken.api.simulation.player.Command;
import com.kraken.api.simulation.tick.Engine;
import com.kraken.api.simulation.world.SimState;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import java.util.concurrent.TimeUnit;

/**
 * One tick of the wave 8 throughput board: a state copy and one {@code advanceInPlace} with no command. The copy is
 * measured too ({@link #copyOnly} isolates it), since every rollout pays it once.
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class AdvanceTickBenchmark {
    private Engine engine;
    private SimState base;
    private SimState work;

    @Setup
    public void setUp() {
        ColosseumPack pack = ColosseumPack.load();
        base = ThroughputBoard.build(pack);
        work = new SimState();
        engine = new Engine();
        engine.beginPass();
    }

    @Benchmark
    public SimState advanceOneTick() {
        work.copyFrom(base);
        engine.advanceInPlace(work, Command.NONE);
        return work;
    }

    @Benchmark
    public SimState copyOnly() {
        work.copyFrom(base);
        return work;
    }
}
