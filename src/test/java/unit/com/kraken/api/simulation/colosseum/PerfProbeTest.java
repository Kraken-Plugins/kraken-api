package unit.com.kraken.api.simulation.colosseum;

import plugins.colosseum.simulation.Frame;
import plugins.colosseum.simulation.Grid;
import plugins.colosseum.simulation.NpcType;
import plugins.colosseum.simulation.Scratch;
import plugins.colosseum.simulation.State;
import plugins.colosseum.simulation.Tick;
import plugins.colosseum.simulation.PlayerCommand;
import plugins.colosseum.simulation.plan.Decision;
import plugins.colosseum.simulation.plan.Planner;
import plugins.colosseum.simulation.plan.PlannerOptions;
import org.junit.jupiter.api.Test;

import java.io.FileWriter;
import java.io.IOException;

import static unit.com.kraken.api.simulation.colosseum.TestArenas.up;

/**
 * Writes planner throughput numbers to a scratch file for performance reporting; the
 * assertions in {@link PlannerTest} are the enforced bounds.
 */
class PerfProbeTest {
    @Test
    void measureThroughput() throws IOException {
        Grid grid = TestArenas.colosimArena();
        Frame frame = TestArenas.frame(
                grid, false,
                NpcType.MANTICORE, NpcType.MANTICORE,
                NpcType.JAVELIN_COLOSSUS, NpcType.JAVELIN_COLOSSUS,
                NpcType.SERPENT_SHAMAN, NpcType.SHOCKWAVE_COLOSSUS,
                NpcType.FREMENNIK_BERSERKER, NpcType.FREMENNIK_SEER, NpcType.FREMENNIK_ARCHER
        );
        State root = TestArenas.state(
                frame, up(7, 15),
                up(20, 5), up(24, 26), up(16, 4), up(18, 28), up(26, 15),
                up(4, 26), up(16, 16), up(17, 17), up(15, 17)
        );

        // Raw advance throughput.
        Scratch scratch = new Scratch();
        State work = new State();
        for (int i = 0; i < 30_000; i++) {
            work.copyFrom(root);
            Tick.advance(work, PlayerCommand.NONE, scratch);
        }
        long t0 = System.nanoTime();
        int advances = 200_000;
        for (int i = 0; i < advances; i++) {
            if ((i & 15) == 0) {
                work.copyFrom(root);
            }
            Tick.advance(work, PlayerCommand.NONE, scratch);
        }
        long advanceNanos = System.nanoTime() - t0;

        Planner planner = new Planner();
        PlannerOptions options = PlannerOptions.defaults();
        for (int i = 0; i < 30; i++) {
            planner.plan(root, options);
        }
        long best = Long.MAX_VALUE;
        int rollouts = 0;
        for (int i = 0; i < 10; i++) {
            Decision decision = planner.plan(root, options);
            if (decision.getElapsedNanos() < best) {
                best = decision.getElapsedNanos();
                rollouts = decision.getRollouts();
            }
        }

        try (FileWriter writer = new FileWriter(System.getProperty("kraken.perf.out", "build/colo-perf.txt"))) {
            writer.write(String.format(
                    "advance: %.2f us/tick (9 NPCs)%nplanner: %d rollouts x %d ticks in %.1f ms (budget 15 ms)%n",
                    advanceNanos / 1000.0 / advances,
                    rollouts,
                    options.getHorizonTicks(),
                    best / 1_000_000.0
            ));
        }
    }
}
