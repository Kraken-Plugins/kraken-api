package com.kraken.api.simulation;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.query.npc.NpcEntity;
import com.kraken.api.service.movement.MovementService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

import java.util.Objects;

/**
 * Converts decision-tree simulation results into executable in-game movement and interaction actions.
 */
@Singleton
public final class SimulationDecisionAdapter {

    @Getter
    @AllArgsConstructor
    public static final class ExecutableAction {
        private final SimulationAction simulationAction;
        private final WorldPoint movementDestination;
        private final Integer targetNpcIndex;
        private final String interactionAction;
        private final double score;
        private final int exploredNodes;

        public boolean hasMovement() {
            return movementDestination != null;
        }

        public boolean hasInteraction() {
            return targetNpcIndex != null
                    && interactionAction != null
                    && !interactionAction.trim().isEmpty();
        }
    }

    private final Context ctx;
    private final MovementService movementService;
    private final SimulationEngine engine;

    @Inject
    public SimulationDecisionAdapter(Context ctx, MovementService movementService) {
        this.ctx = Objects.requireNonNull(ctx, "ctx");
        this.movementService = Objects.requireNonNull(movementService, "movementService");
        this.engine = new SimulationEngine();
    }

    /**
     * Converts a decision result into a movement-only executable action.
     */
    public ExecutableAction adapt(DecisionTreeSearch.Result result, SimulationState rootState) {
        return adapt(result, rootState, null, 1);
    }

    /**
     * Converts a decision result into movement and optional NPC interaction.
     *
     * @param result Decision tree result.
     * @param rootState State used as the decision root.
     * @param interactionAction NPC action to execute (for example "Attack"), null/empty disables interactions.
     * @param interactionDistance Chebyshev distance for selecting an interaction target.
     * @return Executable action translated from the simulation decision.
     */
    public ExecutableAction adapt(
            DecisionTreeSearch.Result result,
            SimulationState rootState,
            String interactionAction,
            int interactionDistance
    ) {
        if (result == null) {
            throw new IllegalArgumentException("result cannot be null");
        }
        if (rootState == null) {
            throw new IllegalArgumentException("rootState cannot be null");
        }

        SimulationAction chosenAction = result.getBestAction() == null
                ? SimulationAction.WAIT
                : result.getBestAction();

        WorldPoint movementDestination = null;
        if (engine.canApplyPlayerAction(rootState, chosenAction)) {
            WorldPoint current = rootState.getPlayerWorldPoint();
            WorldPoint candidate = chosenAction.destinationFrom(current);
            if (!candidate.equals(current)) {
                movementDestination = candidate;
            }
        }

        Integer npcIndex = null;
        String normalizedInteraction = normalizeInteraction(interactionAction);
        if (normalizedInteraction != null) {
            int maxDistance = Math.max(1, interactionDistance);
            SimulationState postActionState = engine.simulateTickCopy(rootState, chosenAction);
            int npcSlot = selectNpcInteractionTarget(postActionState, maxDistance);
            if (npcSlot >= 0) {
                npcIndex = postActionState.getNpcIndex(npcSlot);
            }
        }

        return new ExecutableAction(
                chosenAction,
                movementDestination,
                npcIndex,
                normalizedInteraction,
                result.getBestScore(),
                result.getExploredNodes()
        );
    }

    /**
     * Executes movement and optional NPC interaction in the game client.
     *
     * @return true when at least one action was executed.
     */
    public boolean execute(ExecutableAction action) {
        if (action == null) {
            return false;
        }

        Boolean executed = ctx.runOnClientThread(() -> {
            boolean moved = false;
            boolean interacted = false;

            if (action.hasMovement()) {
                movementService.moveTo(action.getMovementDestination());
                moved = true;
            }

            if (action.hasInteraction()) {
                NpcEntity npc = ctx.npcs()
                        .filter(n -> n != null && n.raw() != null && n.raw().getIndex() == action.getTargetNpcIndex())
                        .first();
                if (npc != null) {
                    interacted = npc.interact(action.getInteractionAction());
                }
            }

            return moved || interacted;
        });

        return Boolean.TRUE.equals(executed);
    }

    private int selectNpcInteractionTarget(SimulationState state, int interactionDistance) {
        int playerX = state.getPlayerX();
        int playerY = state.getPlayerY();

        int bestSlot = -1;
        int bestDistance = Integer.MAX_VALUE;
        for (int slot = 0; slot < state.getNpcCount(); slot++) {
            if (!state.isNpcActive(slot)) {
                continue;
            }

            int dx = Math.abs(state.getNpcX(slot) - playerX);
            int dy = Math.abs(state.getNpcY(slot) - playerY);
            int chebyshev = Math.max(dx, dy);
            if (chebyshev > interactionDistance) {
                continue;
            }

            if (chebyshev < bestDistance) {
                bestDistance = chebyshev;
                bestSlot = slot;
            }
        }

        return bestSlot;
    }

    private String normalizeInteraction(String interactionAction) {
        if (interactionAction == null) {
            return null;
        }
        String trimmed = interactionAction.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
