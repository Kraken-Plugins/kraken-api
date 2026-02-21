package unit.com.kraken.api.simulation;

import com.kraken.api.simulation.*;
import com.kraken.api.simulation.snapshot.SimulationNpcSnapshot;
import com.kraken.api.simulation.snapshot.SimulationPlayerSnapshot;
import com.kraken.api.simulation.snapshot.SimulationSnapshot;
import com.kraken.api.simulation.tree.DecisionTreeSearch;
import com.kraken.api.simulation.tree.SimulationTree;
import com.kraken.api.simulation.tree.SimulationTreeOptions;
import net.runelite.api.CollisionDataFlag;
import net.runelite.api.Prayer;
import net.runelite.api.coords.WorldPoint;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SimulationEngineTest {
    private static final int BASE_X = 3200;
    private static final int BASE_Y = 3200;
    private static final int PLANE = 0;

    private final SimulationEngine engine = new SimulationEngine();

    @Test
    void simulationTreeSupportsLongHorizons() {
        SimulationSnapshot snapshot = snapshot(
                new WorldPoint(BASE_X + 3, BASE_Y + 3, PLANE),
                List.of(npc(1, 1000, BASE_X + 5, BASE_Y + 5)),
                flags -> {
                }
        );
        SimulationScenario scenario = new SimulationScenario(
                snapshot,
                Map.of(1000, new SimulationNpcProfile(1, NpcAttackStyle.MELEE, 4, 8, false))
        );

        SimulationTree tree = engine.generateOutcomeTree(
                scenario,
                SimulationTreeOptions.defaults()
                        .withTicks(16)
                        .withMaxNodes(2000)
                        .withMaxActionsPerNode(30)
                        .withMaxNodes(20),
                (state, depthRemaining) -> Collections.emptyList()
        );

        assertNotNull(tree);
        assertTrue(tree.getNodeCount() > 1);
        assertTrue(tree.getMaxDepthReached() >= 10);
    }

    @Test
    void movementExpansionIncludesWalkAndRunDestinations() {
        SimulationSnapshot snapshot = snapshot(
                new WorldPoint(BASE_X + 3, BASE_Y + 3, PLANE),
                Collections.emptyList(),
                flags -> {
                }
        );
        SimulationState state = engine.createState(new SimulationScenario(snapshot, Collections.emptyMap()));

        List<SimulationAction> actions = engine.generateCandidateActions(
                state,
                3,
                SimulationTreeOptions.defaults()
                        .withMovementRadius(2)
                        .withMovementMode(SimulationMovementMode.RADIUS)
                        .withMaxActionsPerNode(30)
                        .withMaxNodes(20),
                (s, depth) -> Collections.emptyList()
        );

        long walkMoves = actions.stream().filter(a -> a.isMovement() && !a.isRun()).count();
        long runMoves = actions.stream().filter(a -> a.isMovement() && a.isRun()).count();
        assertTrue(walkMoves > 0);
        assertTrue(runMoves > 0);
    }

    @Test
    void intelligentNpcPathingNavigatesAroundCollision() {
        SimulationSnapshot snapshot = snapshot(
                new WorldPoint(BASE_X + 6, BASE_Y + 2, PLANE),
                List.of(npc(7, 2000, BASE_X + 2, BASE_Y + 2)),
                flags -> {
                    flags[3][2] = CollisionDataFlag.BLOCK_MOVEMENT_FULL;
                    flags[4][2] = CollisionDataFlag.BLOCK_MOVEMENT_FULL;
                    flags[5][2] = CollisionDataFlag.BLOCK_MOVEMENT_FULL;
                }
        );

        SimulationScenario scenario = new SimulationScenario(
                snapshot,
                Map.of(2000, new SimulationNpcProfile(1, NpcAttackStyle.MELEE, 4, 10, true))
        );
        SimulationState state = engine.createState(scenario);

        List<WorldPoint> predicted = engine.predictNpcGreedyPathToPlayer(state, 0, 6);
        assertFalse(predicted.isEmpty());
        assertTrue(predicted.stream().anyMatch(point -> point.getY() != BASE_Y + 2));
    }

    @Test
    void decisionSearchReturnsActionableBestMoveTile() {
        SimulationSnapshot snapshot = snapshot(
                new WorldPoint(BASE_X + 3, BASE_Y + 3, PLANE),
                List.of(npc(2, 3000, BASE_X + 3, BASE_Y + 5)),
                flags -> {
                }
        );

        SimulationScenario scenario = new SimulationScenario(
                snapshot,
                Map.of(3000, new SimulationNpcProfile(1, NpcAttackStyle.MELEE, 4, 8, false))
        );
        SimulationTree tree = engine.generateOutcomeTree(
                scenario,
                SimulationTreeOptions.defaults()
                        .withTicks(4)
                        .withMovementRadius(3)
                        .withMovementMode(SimulationMovementMode.RADIUS)
                        .withMaxActionsPerNode(30)
                        .withMaxNodes(20),
                (state, depthRemaining) -> List.of()
        );

        DecisionTreeSearch search = new DecisionTreeSearch();
        DecisionTreeSearch.Result result = search.search(
                tree,
                node -> {
                    SimulationState state = node.getState();
                    int distance = Math.abs(state.getNpcX(0) - state.getPlayerX()) + Math.abs(state.getNpcY(0) - state.getPlayerY());
                    return distance;
                }
        );

        assertNotNull(result);
        assertNotNull(result.getBestAction());
        assertTrue(result.getBestAction().isMovement() || result.getBestAction().isWait());
        assertNotNull(result.getBestPlayerWorldPoint());
    }

    @Test
    void prayerSwitchPreventsIncomingDamage() {
        SimulationSnapshot snapshot = snapshot(
                new WorldPoint(BASE_X + 4, BASE_Y + 1, PLANE),
                List.of(npc(7, 4000, BASE_X + 1, BASE_Y + 1)),
                flags -> {
                }
        );

        SimulationScenario scenario = new SimulationScenario(
                snapshot,
                Map.of(4000, new SimulationNpcProfile(10, NpcAttackStyle.MAGIC, 4, 12, false))
        );

        SimulationState noPrayer = engine.createState(scenario);
        engine.simulateTick(noPrayer, SimulationAction.WAIT);
        assertEquals(87, noPrayer.getPlayerHitpoints());

        SimulationState withPrayer = engine.createState(scenario);
        withPrayer.setActiveProtectionPrayer(Prayer.PROTECT_FROM_MAGIC);
        engine.simulateTick(withPrayer, SimulationAction.WAIT);
        assertEquals(99, withPrayer.getPlayerHitpoints());
    }

    private SimulationNpcSnapshot npc(int index, int id, int x, int y) {
        return new SimulationNpcSnapshot(index, id, 1, new WorldPoint(x, y, PLANE));
    }

    private SimulationSnapshot snapshot(
            WorldPoint playerPoint,
            List<SimulationNpcSnapshot> npcs,
            java.util.function.Consumer<int[][]> flagsMutator
    ) {
        int[][] flags = new int[10][10];
        if (flagsMutator != null) {
            flagsMutator.accept(flags);
        }
        SimulationPlayerSnapshot player = new SimulationPlayerSnapshot(
                playerPoint,
                99,
                99,
                null,
                Map.of(),
                Collections.emptySet()
        );
        return new SimulationSnapshot(0, PLANE, BASE_X, BASE_Y, flags, player, npcs);
    }
}
