package com.kraken.api.simulation;

import net.runelite.api.Prayer;
import net.runelite.api.coords.WorldPoint;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimulationActionPolicyTest {
    private static final int BASE_X = 3200;
    private static final int BASE_Y = 3200;
    private static final int PLANE = 0;

    @Test
    void policyAggregatesActionsAndScoringRules() {
        SimulationEngine engine = new SimulationEngine();
        SimulationState state = createEmptyState();

        SimulationActionPolicy policy = SimulationActionPolicy.builder()
                .addActionProvider(ctx -> List.of(SimulationAction.WAIT, SimulationAction.switchPrayer(Prayer.PROTECT_FROM_MELEE)))
                .addActionProvider(ctx -> List.of(SimulationAction.WAIT, SimulationAction.NORTH))
                .addScoringRule(ctx -> 10.0)
                .addScoringRule(ctx -> -3.5)
                .build();

        SimulationActionPolicyContext context = new SimulationActionPolicyContext(engine, state, 2);
        List<SimulationAction> actions = policy.generateActions(context);
        double score = policy.evaluate(context);

        assertEquals(3, actions.size());
        assertTrue(actions.contains(SimulationAction.WAIT));
        assertTrue(actions.contains(SimulationAction.NORTH));
        assertTrue(actions.contains(SimulationAction.switchPrayer(Prayer.PROTECT_FROM_MELEE)));
        assertEquals(6.5, score, 0.0001);
    }

    @Test
    void policyDefaultsToWalkActionsWhenNoProvidersConfigured() {
        SimulationEngine engine = new SimulationEngine();
        SimulationState state = createEmptyState();

        SimulationActionPolicy policy = SimulationActionPolicy.builder().build();
        List<SimulationAction> actions = policy.generateActions(new SimulationActionPolicyContext(engine, state, 1));

        assertFalse(actions.isEmpty());
        assertEquals(SimulationAction.standardWalkActions().size(), actions.size());
    }

    @Test
    void policyRetainsAdapterAndExecutionConfig() {
        SimulationDecisionAdapter.AdaptOptions adaptOptions =
                new SimulationDecisionAdapter.AdaptOptions("Attack", 1, 8);

        SimulationActionPolicy policy = SimulationActionPolicy.builder()
                .adaptOptions(adaptOptions)
                .allowedExecutionSteps(Set.of(
                        SimulationDecisionAdapter.ExecutableStepType.MOVE,
                        SimulationDecisionAdapter.ExecutableStepType.SWITCH_PRAYER
                ))
                .build();

        assertNotNull(policy.getCaptureOptions());
        assertEquals(adaptOptions, policy.getAdaptOptions());
        assertTrue(policy.getAllowedExecutionSteps().contains(SimulationDecisionAdapter.ExecutableStepType.MOVE));
        assertTrue(policy.getAllowedExecutionSteps().contains(SimulationDecisionAdapter.ExecutableStepType.SWITCH_PRAYER));
        assertFalse(policy.getAllowedExecutionSteps().contains(SimulationDecisionAdapter.ExecutableStepType.CAST_SPELL));
    }

    private SimulationState createEmptyState() {
        SimulationSnapshot snapshot = new SimulationSnapshot(
                0,
                PLANE,
                BASE_X,
                BASE_Y,
                new int[8][8],
                new WorldPoint(BASE_X + 3, BASE_Y + 3, PLANE),
                SimulationPlayerSnapshot.empty(),
                List.of()
        );
        return snapshot.createState();
    }
}
