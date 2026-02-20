package com.kraken.api.simulation;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.query.container.inventory.InventoryEntity;
import com.kraken.api.query.equipment.EquipmentEntity;
import com.kraken.api.query.npc.NpcEntity;
import com.kraken.api.service.magic.CastableSpell;
import com.kraken.api.service.magic.MagicService;
import com.kraken.api.service.movement.MovementService;
import com.kraken.api.service.prayer.PrayerService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.Prayer;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Converts decision-tree simulation results into executable game actions.
 */
@Singleton
public final class SimulationDecisionAdapter {

    /**
     * Supported executable step types.
     */
    public enum ExecutableStepType {
        MOVE,
        NPC_INTERACT,
        SWITCH_PRAYER,
        EQUIP_ITEM,
        INVENTORY_INTERACT,
        CAST_SPELL
    }

    /**
     * Runtime step payload translated from simulation decisions.
     */
    @Getter
    @AllArgsConstructor
    public static final class ExecutableStep {
        private final ExecutableStepType type;
        private final WorldPoint movementDestination;
        private final Integer targetNpcIndex;
        private final String interactionAction;
        private final Prayer prayer;
        private final Integer itemId;
        private final CastableSpell spell;

        /**
         * Creates movement step.
         *
         * @param destination movement destination in world coordinates.
         * @return executable movement step.
         */
        public static ExecutableStep move(WorldPoint destination) {
            return new ExecutableStep(ExecutableStepType.MOVE, destination, null, null, null, null, null);
        }

        /**
         * Creates npc interaction step.
         *
         * @param npcIndex RuneLite npc index to target.
         * @param action npc interaction menu action text.
         * @return executable npc-interaction step.
         */
        public static ExecutableStep npcInteract(int npcIndex, String action) {
            return new ExecutableStep(ExecutableStepType.NPC_INTERACT, null, npcIndex, action, null, null, null);
        }

        /**
         * Creates prayer switch step.
         *
         * @param prayer overhead prayer to activate.
         * @return executable prayer-switch step.
         */
        public static ExecutableStep switchPrayer(Prayer prayer) {
            return new ExecutableStep(ExecutableStepType.SWITCH_PRAYER, null, null, null, prayer, null, null);
        }

        /**
         * Creates equipment step.
         *
         * @param itemId inventory item id to equip.
         * @return executable equip-item step.
         */
        public static ExecutableStep equipItem(int itemId) {
            return new ExecutableStep(ExecutableStepType.EQUIP_ITEM, null, null, null, null, itemId, null);
        }

        /**
         * Creates inventory interaction step.
         *
         * @param itemId inventory item id to interact with.
         * @param action interaction action text (for example, Eat/Drink/Use).
         * @return executable inventory-interaction step.
         */
        public static ExecutableStep inventoryInteract(int itemId, String action) {
            return new ExecutableStep(ExecutableStepType.INVENTORY_INTERACT, null, null, action, null, itemId, null);
        }

        /**
         * Creates spell cast step.
         *
         * @param spell spell to cast.
         * @param targetNpcIndex optional target npc index, null for untargeted cast.
         * @return executable spell-cast step.
         */
        public static ExecutableStep castSpell(CastableSpell spell, Integer targetNpcIndex) {
            return new ExecutableStep(ExecutableStepType.CAST_SPELL, null, targetNpcIndex, null, null, null, spell);
        }
    }

    /**
     * Action adaptation options for optional runtime interaction steps.
     */
    @Getter
    public static final class AdaptOptions {
        private final String interactionAction;
        private final int interactionDistance;
        private final int spellTargetDistance;

        /**
         * Creates adaptation options.
         *
         * @param interactionAction optional npc interaction action (for example, Attack).
         * @param interactionDistance max interaction selection distance.
         * @param spellTargetDistance max spell target selection distance when spell action has no explicit target.
         */
        public AdaptOptions(String interactionAction, int interactionDistance, int spellTargetDistance) {
            this.interactionAction = normalizeInteraction(interactionAction);
            this.interactionDistance = Math.max(1, interactionDistance);
            this.spellTargetDistance = Math.max(1, spellTargetDistance);
        }

        /**
         * @return options with no optional npc interaction step.
         */
        public static AdaptOptions none() {
            return new AdaptOptions(null, 1, 12);
        }
    }

    /**
     * Runtime action payload translated from a simulation decision.
     */
    @Getter
    public static final class ExecutableAction {
        private final SimulationAction simulationAction;
        private final List<ExecutableStep> steps;
        private final WorldPoint movementDestination;
        private final Integer targetNpcIndex;
        private final String interactionAction;
        private final double score;
        private final int exploredNodes;

        ExecutableAction(
                SimulationAction simulationAction,
                List<ExecutableStep> steps,
                double score,
                int exploredNodes
        ) {
            this.simulationAction = simulationAction;
            this.steps = steps == null ? Collections.emptyList() : List.copyOf(steps);
            this.score = score;
            this.exploredNodes = exploredNodes;

            WorldPoint move = null;
            Integer npcIndex = null;
            String npcAction = null;
            for (ExecutableStep step : this.steps) {
                if (move == null && step.getType() == ExecutableStepType.MOVE) {
                    move = step.getMovementDestination();
                }
                if (npcIndex == null && step.getType() == ExecutableStepType.NPC_INTERACT) {
                    npcIndex = step.getTargetNpcIndex();
                    npcAction = step.getInteractionAction();
                }
            }
            this.movementDestination = move;
            this.targetNpcIndex = npcIndex;
            this.interactionAction = npcAction;
        }

        /**
         * @return true when this action includes a movement destination.
         */
        public boolean hasMovement() {
            return movementDestination != null;
        }

        /**
         * @return true when this action includes a target and interaction option.
         */
        public boolean hasInteraction() {
            return targetNpcIndex != null
                    && interactionAction != null
                    && !interactionAction.trim().isEmpty();
        }

        /**
         * @return true when this action includes a prayer switch step.
         */
        public boolean hasPrayerSwitch() {
            return steps.stream().anyMatch(step -> step.getType() == ExecutableStepType.SWITCH_PRAYER);
        }

        /**
         * @return true when this action includes an equipment swap step.
         */
        public boolean hasEquipmentSwap() {
            return steps.stream().anyMatch(step -> step.getType() == ExecutableStepType.EQUIP_ITEM);
        }

        /**
         * @return true when this action includes an inventory interaction step.
         */
        public boolean hasInventoryInteraction() {
            return steps.stream().anyMatch(step -> step.getType() == ExecutableStepType.INVENTORY_INTERACT);
        }

        /**
         * @return true when this action includes a spell cast step.
         */
        public boolean hasSpellCast() {
            return steps.stream().anyMatch(step -> step.getType() == ExecutableStepType.CAST_SPELL);
        }
    }

    private final Context ctx;
    private final MovementService movementService;
    private final PrayerService prayerService;
    private final MagicService magicService;
    private final SimulationEngine engine;

    /**
     * Constructs a decision adapter for converting and executing simulation outcomes.
     *
     * @param ctx shared API context.
     * @param movementService movement executor service.
     * @param prayerService prayer service.
     * @param magicService magic service.
     */
    @Inject
    public SimulationDecisionAdapter(
            Context ctx,
            MovementService movementService,
            PrayerService prayerService,
            MagicService magicService
    ) {
        this.ctx = Objects.requireNonNull(ctx, "ctx");
        this.movementService = Objects.requireNonNull(movementService, "movementService");
        this.prayerService = Objects.requireNonNull(prayerService, "prayerService");
        this.magicService = Objects.requireNonNull(magicService, "magicService");
        this.engine = new SimulationEngine();
    }

    /**
     * Converts a decision result into an executable action with no optional interaction step.
     *
     * @param result decision tree search result.
     * @param rootState root simulation state.
     * @return executable action.
     */
    public ExecutableAction adapt(DecisionTreeSearch.Result result, SimulationState rootState) {
        return adapt(result, rootState, AdaptOptions.none());
    }

    /**
     * Converts a decision result into an executable action with optional npc interaction.
     *
     * @param result decision tree result.
     * @param rootState root simulation state.
     * @param interactionAction npc action to execute (for example "Attack"), null/empty disables interactions.
     * @param interactionDistance Chebyshev distance for selecting an interaction target.
     * @return executable action.
     */
    public ExecutableAction adapt(
            DecisionTreeSearch.Result result,
            SimulationState rootState,
            String interactionAction,
            int interactionDistance
    ) {
        return adapt(result, rootState, new AdaptOptions(interactionAction, interactionDistance, 12));
    }

    /**
     * Converts a decision result into an executable action with configurable adaptation options.
     *
     * @param result decision tree result.
     * @param rootState root simulation state.
     * @param options action adaptation options.
     * @return executable action.
     */
    public ExecutableAction adapt(
            DecisionTreeSearch.Result result,
            SimulationState rootState,
            AdaptOptions options
    ) {
        if (result == null) {
            throw new IllegalArgumentException("result cannot be null");
        }
        if (rootState == null) {
            throw new IllegalArgumentException("rootState cannot be null");
        }

        AdaptOptions adaptOptions = options == null ? AdaptOptions.none() : options;

        SimulationAction chosenAction = result.getBestAction() == null
                ? SimulationAction.WAIT
                : result.getBestAction();

        List<ExecutableStep> steps = new ArrayList<>();
        addSimulationActionStep(steps, rootState, chosenAction, adaptOptions);

        SimulationState postActionState = engine.simulateTickCopy(rootState, chosenAction);

        String normalizedInteraction = adaptOptions.getInteractionAction();
        if (normalizedInteraction != null) {
            int npcSlot = selectNpcInteractionTarget(postActionState, adaptOptions.getInteractionDistance());
            if (npcSlot >= 0) {
                steps.add(ExecutableStep.npcInteract(postActionState.getNpcIndex(npcSlot), normalizedInteraction));
            }
        }

        return new ExecutableAction(
                chosenAction,
                steps,
                result.getBestScore(),
                result.getExploredNodes()
        );
    }

    /**
     * Executes all action steps in order.
     *
     * @param action executable action to perform.
     * @return true when at least one step was executed successfully.
     */
    public boolean execute(ExecutableAction action) {
        return execute(action, EnumSet.allOf(ExecutableStepType.class));
    }

    /**
     * Executes all action steps in order, filtered by allowed step types.
     *
     * @param action executable action to perform.
     * @param allowedStepTypes set of step types permitted for execution.
     * @return true when at least one permitted step was executed successfully.
     */
    public boolean execute(ExecutableAction action, Set<ExecutableStepType> allowedStepTypes) {
        if (action == null || action.getSteps().isEmpty()) {
            return false;
        }
        if (allowedStepTypes == null || allowedStepTypes.isEmpty()) {
            return false;
        }

        Boolean executed = ctx.runOnClientThread(() -> {
            boolean executedAny = false;
            for (ExecutableStep step : action.getSteps()) {
                if (step == null) {
                    continue;
                }
                if (!allowedStepTypes.contains(step.getType())) {
                    continue;
                }
                executedAny |= executeStep(step);
            }
            return executedAny;
        });

        return Boolean.TRUE.equals(executed);
    }

    private void addSimulationActionStep(
            List<ExecutableStep> steps,
            SimulationState rootState,
            SimulationAction action,
            AdaptOptions options
    ) {
        if (action == null) {
            return;
        }

        if (action.isMovement()) {
            if (engine.canApplyPlayerAction(rootState, action)) {
                WorldPoint current = rootState.getPlayerWorldPoint();
                WorldPoint candidate = action.destinationFrom(current);
                if (!candidate.equals(current)) {
                    steps.add(ExecutableStep.move(candidate));
                }
            }
            return;
        }

        switch (action.getType()) {
            case SWITCH_PRAYER:
                if (action.getPrayer() != null) {
                    steps.add(ExecutableStep.switchPrayer(action.getPrayer()));
                }
                break;
            case EQUIP_ITEM:
                if (action.getItemId() != null) {
                    steps.add(ExecutableStep.equipItem(action.getItemId()));
                }
                break;
            case INVENTORY_INTERACT:
                if (action.getItemId() != null) {
                    String inventoryAction = action.getInventoryAction() == null ? "Use" : action.getInventoryAction();
                    steps.add(ExecutableStep.inventoryInteract(action.getItemId(), inventoryAction));
                }
                break;
            case CAST_SPELL:
                if (action.getSpell() != null) {
                    Integer targetNpcIndex = action.getTargetNpcIndex();
                    if (targetNpcIndex == null) {
                        int resolved = resolveSpellTargetNpcIndex(rootState, options.getSpellTargetDistance());
                        if (resolved >= 0) {
                            targetNpcIndex = resolved;
                        }
                    }
                    steps.add(ExecutableStep.castSpell(action.getSpell(), targetNpcIndex));
                }
                break;
            case CUSTOM:
            default:
                break;
        }
    }

    private boolean executeStep(ExecutableStep step) {
        switch (step.getType()) {
            case MOVE:
                if (step.getMovementDestination() == null) {
                    return false;
                }
                movementService.moveTo(step.getMovementDestination());
                return true;
            case NPC_INTERACT:
                return executeNpcInteraction(step.getTargetNpcIndex(), step.getInteractionAction());
            case SWITCH_PRAYER:
                if (step.getPrayer() == null) {
                    return false;
                }
                return prayerService.toggle(step.getPrayer(), true);
            case EQUIP_ITEM:
                return executeEquipItem(step.getItemId());
            case INVENTORY_INTERACT:
                return executeInventoryInteraction(step.getItemId(), step.getInteractionAction());
            case CAST_SPELL:
                return executeSpell(step.getSpell(), step.getTargetNpcIndex());
            default:
                return false;
        }
    }

    private boolean executeNpcInteraction(Integer targetNpcIndex, String interactionAction) {
        if (targetNpcIndex == null || interactionAction == null || interactionAction.trim().isEmpty()) {
            return false;
        }
        NpcEntity npc = ctx.npcs()
                .filter(n -> n != null && n.raw() != null && n.raw().getIndex() == targetNpcIndex)
                .first();
        return npc != null && npc.interact(interactionAction);
    }

    private boolean executeEquipItem(Integer itemId) {
        if (itemId == null) {
            return false;
        }
        EquipmentEntity equipmentEntity = ctx.equipment().inInventory().withId(itemId).first();
        return equipmentEntity != null && equipmentEntity.wieldOrWear();
    }

    private boolean executeInventoryInteraction(Integer itemId, String interactionAction) {
        if (itemId == null) {
            return false;
        }
        InventoryEntity inventoryEntity = ctx.inventory().withId(itemId).first();
        if (inventoryEntity == null) {
            return false;
        }
        String action = interactionAction == null || interactionAction.trim().isEmpty() ? "Use" : interactionAction;
        return inventoryEntity.interact(action);
    }

    private boolean executeSpell(CastableSpell spell, Integer targetNpcIndex) {
        if (spell == null) {
            return false;
        }
        if (targetNpcIndex != null) {
            NpcEntity npc = ctx.npcs()
                    .filter(n -> n != null && n.raw() != null && n.raw().getIndex() == targetNpcIndex)
                    .first();
            if (npc != null && npc.raw() != null) {
                return magicService.castOn(spell, npc.raw());
            }
        }
        return magicService.cast(spell);
    }

    private int resolveSpellTargetNpcIndex(SimulationState state, int maxDistance) {
        int slot = selectNpcInteractionTarget(state, Math.max(1, maxDistance));
        return slot < 0 ? -1 : state.getNpcIndex(slot);
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

    private static String normalizeInteraction(String interactionAction) {
        if (interactionAction == null) {
            return null;
        }
        String trimmed = interactionAction.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
