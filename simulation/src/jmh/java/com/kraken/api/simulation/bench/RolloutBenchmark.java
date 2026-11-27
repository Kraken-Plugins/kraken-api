package com.kraken.api.simulation.bench;

import com.kraken.api.simulation.colosseum.ColosseumPack;
import com.kraken.api.simulation.colosseum.ThroughputBoard;
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
 * Horizon 10 rollouts on Auto Colosseum V2's throughput board, the unit a planner's search is budgeted in. Rollouts
 * per 15 ms deadline = 15,000,000 / the reported ns per op.
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class RolloutBenchmark {
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
    public SimState rollout() {
        ThroughputBoard.rollout(engine, base, work);
        return work;
    }
}
