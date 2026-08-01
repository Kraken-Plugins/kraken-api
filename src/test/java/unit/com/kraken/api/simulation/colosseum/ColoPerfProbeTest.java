package unit.com.kraken.api.simulation.colosseum;

import plugins.colosseum.simulation.ColoFrame;
import plugins.colosseum.simulation.ColoGrid;
import plugins.colosseum.simulation.ColoNpcType;
import plugins.colosseum.simulation.ColoScratch;
import plugins.colosseum.simulation.ColoState;
import plugins.colosseum.simulation.ColoTick;
import plugins.colosseum.simulation.PlayerCommand;
import plugins.colosseum.simulation.plan.ColoDecision;
import plugins.colosseum.simulation.plan.ColoPlanner;
import plugins.colosseum.simulation.plan.PlannerOptions;
import org.junit.jupiter.api.Test;

import java.io.FileWriter;
import java.io.IOException;

import static unit.com.kraken.api.simulation.colosseum.ColoTestArenas.up;

/**
 * Writes planner throughput numbers to a scratch file for performance reporting; the
 * assertions in {@link ColoPlannerTest} are the enforced bounds.
 */
class ColoPerfProbeTest {
    @Test
    void measureThroughput() throws IOException {
        ColoGrid grid = ColoTestArenas.colosimArena();
        ColoFrame frame = ColoTestArenas.frame(
                grid, false,
                ColoNpcType.MANTICORE, ColoNpcType.MANTICORE,
                ColoNpcType.JAVELIN_COLOSSUS, ColoNpcType.JAVELIN_COLOSSUS,
                ColoNpcType.SERPENT_SHAMAN, ColoNpcType.SHOCKWAVE_COLOSSUS,
                ColoNpcType.FREMENNIK_BERSERKER, ColoNpcType.FREMENNIK_SEER, ColoNpcType.FREMENNIK_ARCHER
        );
        ColoState root = ColoTestArenas.state(
                frame, up(7, 15),
                up(20, 5), up(24, 26), up(16, 4), up(18, 28), up(26, 15),
                up(4, 26), up(16, 16), up(17, 17), up(15, 17)
        );

        // Raw advance throughput.
        ColoScratch scratch = new ColoScratch();
        ColoState work = new ColoState();
        for (int i = 0; i < 30_000; i++) {
            work.copyFrom(root);
            ColoTick.advance(work, PlayerCommand.NONE, scratch);
        }
        long t0 = System.nanoTime();
        int advances = 200_000;
        for (int i = 0; i < advances; i++) {
            if ((i & 15) == 0) {
                work.copyFrom(root);
            }
            ColoTick.advance(work, PlayerCommand.NONE, scratch);
        }
        long advanceNanos = System.nanoTime() - t0;

        ColoPlanner planner = new ColoPlanner();
        PlannerOptions options = PlannerOptions.defaults();
        for (int i = 0; i < 30; i++) {
            planner.plan(root, options);
        }
        long best = Long.MAX_VALUE;
        int rollouts = 0;
        for (int i = 0; i < 10; i++) {
            ColoDecision decision = planner.plan(root, options);
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
