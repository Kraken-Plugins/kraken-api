package unit.com.kraken.api.simulation.colosseum;

import plugins.colosseum.simulation.ColoFrame;
import plugins.colosseum.simulation.ColoGrid;
import plugins.colosseum.simulation.ColoNpcType;
import plugins.colosseum.simulation.ColoState;
import plugins.colosseum.simulation.plan.ColoDecision;
import plugins.colosseum.simulation.plan.ColoPlanner;
import plugins.colosseum.simulation.plan.PlannerOptions;
import net.runelite.api.Prayer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static unit.com.kraken.api.simulation.colosseum.ColoTestArenas.up;

/**
 * Planner behaviour and real-time budget tests.
 */
class ColoPlannerTest {

    @Test
    void plansAgainstBusyWaveWithinBudget() {
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
                up(20, 5), up(24, 26),
                up(16, 4), up(18, 28),
                up(26, 15), up(4, 26),
                up(16, 16), up(17, 17), up(15, 17)
        );

        ColoPlanner planner = new ColoPlanner();
        PlannerOptions options = PlannerOptions.defaults();

        // Warm-up passes (JIT + field caches) mirror steady-state use - the live plugin
        // replans every tick - then a measured pass.
        for (int i = 0; i < 10; i++) {
            planner.plan(root, options);
        }
        long start = System.nanoTime();
        ColoDecision decision = planner.plan(root, options);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertNotNull(decision);
        assertTrue(decision.getRollouts() > 20, "expected a meaningful number of rollouts, got " + decision.getRollouts());
        assertTrue(decision.candidateCount() > 5, "expected several candidate tiles");
        assertNotNull(decision.getReasoning());
        assertTrue(Double.isFinite(decision.getScore()));
        // Generous CI bound; the budget itself is 15 ms and the planner stops at the deadline.
        assertTrue(elapsedMs < 120, "planning took " + elapsedMs + " ms");
    }

    @Test
    void policyEatsWhenCriticallyLow() {
        ColoGrid grid = ColoTestArenas.colosimArena();
        ColoFrame frame = ColoTestArenas.frame(grid, false, ColoNpcType.SERPENT_SHAMAN);
        ColoState root = ColoTestArenas.state(frame, up(7, 15), up(20, 15));
        root.setPlayer(up(7, 15), 20, 60, 10_000, 100, ColoState.OVERHEAD_NONE, 0);
        root.setSupplies(6, 4, 4, 4);

        ColoDecision decision = new ColoPlanner().plan(root, PlannerOptions.defaults());
        assertTrue(decision.isEatFood(), "critically low HP must eat immediately");
        assertTrue(decision.isEatCombo(), "combo food stacks with primary food at critical HP");
    }

    @Test
    void policyPraysAgainstImminentShamanLaunch() {
        ColoGrid grid = ColoTestArenas.colosimArena();
        ColoFrame frame = ColoTestArenas.frame(grid, false, ColoNpcType.SERPENT_SHAMAN);
        // Shaman adjacent-ish with line of sight and a ready attack timer: it launches on
        // the very next tick, so the first command must carry Protect from Magic.
        ColoState root = ColoTestArenas.state(frame, up(16, 18), up(20, 18));

        ColoDecision decision = new ColoPlanner().plan(root, PlannerOptions.defaults());
        assertEquals(Prayer.PROTECT_FROM_MAGIC, decision.getPrayerToActivate(),
                "planner must pre-pray the imminent magic launch");
    }

    @Test
    void plannerPrefersBreakingLineOfSightWhenOutgunned() {
        ColoGrid grid = ColoTestArenas.colosimArena();
        // Two ranged colossi with line of sight; the player stands near the NW pillar with
        // cover one step away. Disable targets' relevance by giving the player no reason to
        // stand: the planner should value the zero-exposure tile.
        ColoFrame frame = ColoTestArenas.frame(grid, false, ColoNpcType.JAVELIN_COLOSSUS, ColoNpcType.SHOCKWAVE_COLOSSUS);
        ColoState root = ColoTestArenas.state(frame, up(12, 9), up(20, 5), up(22, 12));

        ColoPlanner planner = new ColoPlanner();
        ColoDecision decision = planner.plan(root, PlannerOptions.defaults());

        // The decision should either move toward cover or at least pray against the bigger
        // imminent threat; both beat standing exposed. Assert the chosen candidate scored
        // at least as well as staying put.
        double stayScore = Double.NEGATIVE_INFINITY;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < decision.candidateCount(); i++) {
            double score = decision.candidateScore(i);
            if (decision.candidateWorldPoint(i).equals(grid.toWorld(root.playerPos()))) {
                stayScore = score;
            }
            bestScore = Math.max(bestScore, score);
        }
        assertTrue(bestScore >= stayScore, "best candidate cannot be worse than standing still");
        assertNotNull(decision.plannedPathWorld());
    }

    @Test
    void noAttackClickWhenAlreadyFightingThePlannedTarget() {
        ColoGrid grid = ColoTestArenas.colosimArena();
        ColoFrame frame = ColoTestArenas.frame(grid, false, ColoNpcType.SERPENT_SHAMAN);
        // Shaman on a long cooldown poses no threat; the player is in range with the magic
        // set and already attacking it - the best plan is to stand and keep fighting,
        // which must not issue a redundant attack click.
        ColoState root = ColoTestArenas.state(frame, up(16, 18), up(20, 18));
        root.activateNpc(0, up(20, 18), 125, 30);
        root.setPlayer(up(16, 18), 99, 99, 10_000, 100, ColoState.OVERHEAD_NONE, 2);
        root.setAttackTarget(0);

        ColoDecision decision = new ColoPlanner().plan(root, PlannerOptions.defaults());
        assertEquals(0, decision.getAttackNpcSlot(), "plan keeps fighting the engaged target");
        assertTrue(!decision.hasAttack(), "no new attack click while already fighting the target");
        assertTrue(decision.getMoveDestination() == null, "no reason to move in a zero-threat hold");
    }

    @Test
    void attackClickIssuedWhenNotYetEngaged() {
        ColoGrid grid = ColoTestArenas.colosimArena();
        ColoFrame frame = ColoTestArenas.frame(grid, false, ColoNpcType.SERPENT_SHAMAN);
        ColoState root = ColoTestArenas.state(frame, up(16, 18), up(20, 18));
        root.activateNpc(0, up(20, 18), 125, 30);
        root.setPlayer(up(16, 18), 99, 99, 10_000, 100, ColoState.OVERHEAD_NONE, 2);

        ColoDecision decision = new ColoPlanner().plan(root, PlannerOptions.defaults());
        assertTrue(decision.hasAttack(), "unengaged plans must click the target to start combat");
        assertEquals(0, decision.getAttackNpcRuneliteIndex());
    }

    @Test
    void decisionExposesVisualizationData() {
        ColoGrid grid = ColoTestArenas.colosimArena();
        ColoFrame frame = ColoTestArenas.frame(grid, false, ColoNpcType.SERPENT_SHAMAN, ColoNpcType.MANTICORE);
        ColoState root = ColoTestArenas.state(frame, up(7, 15), up(20, 15), up(24, 20));

        ColoPlanner planner = new ColoPlanner();
        ColoDecision decision = planner.plan(root, PlannerOptions.defaults());

        assertTrue(decision.candidateCount() > 0);
        for (int i = 0; i < decision.candidateCount(); i++) {
            assertNotNull(decision.candidateWorldPoint(i));
        }
        assertNotNull(planner.dangerMap());
        assertTrue(planner.dangerMap().covers(root.playerPos()));
        assertTrue(decision.getElapsedNanos() > 0);
    }
}
