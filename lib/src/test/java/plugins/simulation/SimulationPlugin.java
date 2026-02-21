package plugins.simulation;

import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.service.magic.CastableSpell;
import com.kraken.api.service.magic.spellbook.Standard;
import com.kraken.api.simulation.DecisionTreeSearch;
import com.kraken.api.simulation.NpcAttackStyle;
import com.kraken.api.simulation.SimulationAction;
import com.kraken.api.simulation.SimulationActionPolicy;
import com.kraken.api.simulation.SimulationDecisionAdapter;
import com.kraken.api.simulation.SimulationEngine;
import com.kraken.api.simulation.SimulationSnapshot;
import com.kraken.api.simulation.SimulationSnapshotService;
import com.kraken.api.simulation.SimulationState;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GameState;
import net.runelite.api.Prayer;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Singleton
@PluginDescriptor(
        name = "Simulation Sandbox",
        description = "Generic OSRS simulation plugin with decision-tree search overlays and execution adapter.",
        tags = {"kraken", "simulation", "overlay", "decision"}
)
public class SimulationPlugin extends Plugin {
    private static final List<SimulationAction> RUN_ACTIONS = Collections.unmodifiableList(Arrays.asList(
            SimulationAction.run(0, 1),
            SimulationAction.run(0, -1),
            SimulationAction.run(1, 0),
            SimulationAction.run(-1, 0),
            SimulationAction.run(1, 1),
            SimulationAction.run(-1, 1),
            SimulationAction.run(1, -1),
            SimulationAction.run(-1, -1)
    ));

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private SimulationPluginConfig config;

    @Inject
    private SimulationSceneOverlay sceneOverlay;

    @Inject
    private SimulationInfoOverlay infoOverlay;

    @Inject
    private Context context;

    @Inject
    private SimulationDecisionAdapter decisionAdapter;

    @Getter
    private final SimulationEngine engine = new SimulationEngine();

    @Getter
    private SimulationSnapshot lastSnapshot;

    @Getter
    private SimulationState rootState;

    @Getter
    private SimulationState bestActionState;

    @Getter
    private DecisionTreeSearch.Result lastDecisionResult;

    @Getter
    private SimulationDecisionAdapter.ExecutableAction lastExecutableAction;

    @Getter
    private SimulationActionPolicy lastPolicy;

    @Getter
    private long lastSearchMicros;

    @Getter
    private int rootThreatCount;

    @Getter
    private int bestThreatCount;

    @Getter
    private int rootAttackThreatCount;

    @Getter
    private int bestAttackThreatCount;

    @Getter
    private int rootUnprotectedThreatCount;

    @Getter
    private int bestUnprotectedThreatCount;

    @Getter
    private Prayer rootRecommendedPrayer;

    @Getter
    private Prayer bestRecommendedPrayer;

    @Getter
    private String lastError;

    @Getter
    private final Map<Integer, List<WorldPoint>> npcPredictedPaths = new HashMap<>();

    @Getter
    private final Map<Integer, List<WorldPoint>> npcLineOfSightTiles = new HashMap<>();

    @Provides
    SimulationPluginConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(SimulationPluginConfig.class);
    }

    @Override
    protected void startUp() {
        context.initializePackets();
        syncOverlayState();
        clearTransientState();
    }

    @Override
    protected void shutDown() {
        overlayManager.remove(sceneOverlay);
        overlayManager.remove(infoOverlay);
        clearTransientState();
    }

    @Subscribe
    private void onConfigChanged(ConfigChanged event) {
        if (!"krakenSimulation".equals(event.getGroup())) {
            return;
        }
        syncOverlayState();
    }

    @Subscribe
    private void onGameStateChanged(GameStateChanged event) {
        GameState state = event.getGameState();
        if (state != GameState.LOGGED_IN) {
            clearTransientState();
        }
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        if (!config.enabled()) {
            clearTransientState();
            return;
        }

        try {
            runSimulationTick();
            lastError = null;
        } catch (Exception ex) {
            lastError = ex.getMessage();
            log.debug("Simulation plugin tick failed", ex);
        }
    }

    private void runSimulationTick() {
        SimulationActionPolicy policy = buildPolicy();
        lastPolicy = policy;

        lastSnapshot = SimulationSnapshotService.capture(policy.getCaptureOptions());
        rootState = lastSnapshot.createState();

        DecisionTreeSearch search = new DecisionTreeSearch(engine, Math.max(64, config.maxSearchNodes()));
        long startedNanos = System.nanoTime();
        lastDecisionResult = search.search(
                rootState,
                Math.max(1, config.searchDepth()),
                policy.toActionGenerator(engine),
                policy.toStateEvaluator(engine)
        );
        lastSearchMicros = (System.nanoTime() - startedNanos) / 1000L;

        bestActionState = engine.simulateTickCopy(rootState, lastDecisionResult.getBestAction());
        rootThreatCount = engine.countNpcsWithLineOfSightToPlayer(rootState);
        bestThreatCount = engine.countNpcsWithLineOfSightToPlayer(bestActionState);
        rootAttackThreatCount = engine.countNpcsAbleToAttackPlayer(rootState);
        bestAttackThreatCount = engine.countNpcsAbleToAttackPlayer(bestActionState);
        rootUnprotectedThreatCount = engine.countUnprotectedNpcThreats(rootState);
        bestUnprotectedThreatCount = engine.countUnprotectedNpcThreats(bestActionState);
        rootRecommendedPrayer = engine.recommendProtectionPrayer(rootState);
        bestRecommendedPrayer = engine.recommendProtectionPrayer(bestActionState);

        updateNpcVisualizationCaches(rootState);

        lastExecutableAction = decisionAdapter.adapt(lastDecisionResult, rootState, policy.getAdaptOptions());

        if (config.autoExecuteBestAction()) {
            decisionAdapter.execute(lastExecutableAction, policy.getAllowedExecutionSteps());
        }
    }

    private SimulationActionPolicy buildPolicy() {
        int radius = Math.max(1, config.snapshotNpcRadius());
        Map<Integer, SimulationSnapshotService.NpcMetadata> npcOverrides = parseNpcCombatOverrides(config.npcCombatOverrides());
        Map<Integer, Integer> foodHealMapping = parseFoodHealOverrides(config.foodHealingOverrides());

        SimulationSnapshotService.CaptureOptions captureOptions = new SimulationSnapshotService.CaptureOptions()
                .withNpcRadius(radius)
                .withFoodHealingByItemId(foodHealMapping);
        if (!npcOverrides.isEmpty()) {
            captureOptions = captureOptions.withNpcMetadataProvider((npc, composition) -> npcOverrides.get(npc.getId()));
        }

        String optionalNpcInteraction = config.executeNpcInteraction() ? config.interactionAction() : null;
        SimulationDecisionAdapter.AdaptOptions adaptOptions = new SimulationDecisionAdapter.AdaptOptions(
                optionalNpcInteraction,
                config.interactionDistance(),
                config.spellTargetDistance()
        );

        return SimulationActionPolicy.builder()
                .captureOptions(captureOptions)
                .adaptOptions(adaptOptions)
                .allowedExecutionSteps(buildExecutionAllowList())
                .addActionProvider(ctx -> generateCandidateActions(ctx.getState()))
                .addScoringRule(ctx -> evaluateState(ctx.getState()))
                .build();
    }

    private Set<SimulationDecisionAdapter.ExecutableStepType> buildExecutionAllowList() {
        EnumSet<SimulationDecisionAdapter.ExecutableStepType> allowed = EnumSet.of(SimulationDecisionAdapter.ExecutableStepType.MOVE);
        if (config.executeNpcInteraction()) {
            allowed.add(SimulationDecisionAdapter.ExecutableStepType.NPC_INTERACT);
        }
        if (config.executePrayerSwitches()) {
            allowed.add(SimulationDecisionAdapter.ExecutableStepType.SWITCH_PRAYER);
        }
        if (config.executeGearSwaps()) {
            allowed.add(SimulationDecisionAdapter.ExecutableStepType.EQUIP_ITEM);
        }
        if (config.executeInventoryActions()) {
            allowed.add(SimulationDecisionAdapter.ExecutableStepType.INVENTORY_INTERACT);
        }
        if (config.executeSpells()) {
            allowed.add(SimulationDecisionAdapter.ExecutableStepType.CAST_SPELL);
        }
        return allowed;
    }

    private List<SimulationAction> generateCandidateActions(SimulationState state) {
        LinkedHashSet<SimulationAction> all = new LinkedHashSet<>(SimulationAction.standardWalkActions());
        if (config.includeRunActions()) {
            all.addAll(RUN_ACTIONS);
        }

        if (config.includePrayerActions()) {
            Prayer recommended = engine.recommendProtectionPrayer(state);
            if (recommended != null && recommended != state.getActiveProtectionPrayer()) {
                all.add(SimulationAction.switchPrayer(recommended));
            }
        }

        if (config.includeEatActions() && state.getPlayerHitpoints() <= Math.max(1, config.eatAtOrBelowHp())) {
            SimulationAction eatAction = resolveEatAction(state);
            if (eatAction != null) {
                all.add(eatAction);
            }
        }

        if (config.includeGearSwapActions()) {
            int itemId = config.gearSwapItemId();
            if (itemId >= 0 && state.hasInventoryItem(itemId) && !state.isItemEquipped(itemId)) {
                all.add(SimulationAction.equipItem(itemId));
            }
        }

        if (config.includeSpellActions()) {
            SimulationAction spellAction = resolveSpellAction(state);
            if (spellAction != null) {
                all.add(spellAction);
            }
        }

        return all.stream()
                .filter(action -> engine.canApplyPlayerAction(state, action))
                .collect(Collectors.toList());
    }

    private SimulationAction resolveEatAction(SimulationState state) {
        if (state.getPlayerHitpoints() >= state.getPlayerMaxHitpoints()) {
            return null;
        }

        int bestItemId = -1;
        int bestHeal = 0;
        for (Map.Entry<Integer, Integer> entry : state.getFoodHealingByItemId().entrySet()) {
            int itemId = entry.getKey();
            int heal = entry.getValue();
            if (heal <= 0 || !state.hasInventoryItem(itemId)) {
                continue;
            }
            if (heal > bestHeal) {
                bestHeal = heal;
                bestItemId = itemId;
            }
        }

        if (bestItemId < 0) {
            return null;
        }
        return SimulationAction.eat(bestItemId, bestHeal);
    }

    private SimulationAction resolveSpellAction(SimulationState state) {
        CastableSpell spell = resolveConfiguredStandardSpell();
        if (spell == null) {
            return null;
        }

        int targetNpcIndex = nearestNpcIndexWithinDistance(state, Math.max(1, config.spellTargetDistance()));
        if (targetNpcIndex >= 0) {
            return SimulationAction.castSpellOnNpc(spell, targetNpcIndex);
        }
        return SimulationAction.castSpell(spell);
    }

    private CastableSpell resolveConfiguredStandardSpell() {
        String configured = config.standardSpellName();
        if (configured == null || configured.trim().isEmpty()) {
            return null;
        }

        String normalized = configured.trim().toUpperCase(Locale.ROOT);
        try {
            return Standard.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private int nearestNpcIndexWithinDistance(SimulationState state, int maxDistance) {
        int bestNpcIndex = -1;
        int bestDistance = Integer.MAX_VALUE;
        for (int slot = 0; slot < state.getNpcCount(); slot++) {
            if (!state.isNpcActive(slot)) {
                continue;
            }
            int dx = Math.abs(state.getNpcX(slot) - state.getPlayerX());
            int dy = Math.abs(state.getNpcY(slot) - state.getPlayerY());
            int distance = Math.max(dx, dy);
            if (distance > maxDistance) {
                continue;
            }
            if (distance < bestDistance) {
                bestDistance = distance;
                bestNpcIndex = state.getNpcIndex(slot);
            }
        }
        return bestNpcIndex;
    }

    private double evaluateState(SimulationState state) {
        int losThreats = engine.countNpcsWithLineOfSightToPlayer(state);
        int attackThreats = engine.countNpcsAbleToAttackPlayer(state);
        int unprotectedThreats = engine.countUnprotectedNpcThreats(state);
        int nearestNpcDistance = nearestNpcChebyshevDistance(state);

        double score = 0.0;
        score -= (losThreats * 20.0);
        score -= (attackThreats * 25.0);
        score -= (unprotectedThreats * 40.0);
        score += Math.min(nearestNpcDistance, 12) * 2.0;
        score += state.getPlayerHitpoints() * 1.1;

        Prayer recommended = engine.recommendProtectionPrayer(state);
        if (recommended != null && recommended == state.getActiveProtectionPrayer()) {
            score += 24.0;
        }

        if (engine.isPlayerTileSafe(state)) {
            score += 25.0;
        }

        return score;
    }

    private int nearestNpcChebyshevDistance(SimulationState state) {
        int min = Integer.MAX_VALUE;
        for (int slot = 0; slot < state.getNpcCount(); slot++) {
            if (!state.isNpcActive(slot)) {
                continue;
            }
            int dx = Math.abs(state.getNpcX(slot) - state.getPlayerX());
            int dy = Math.abs(state.getNpcY(slot) - state.getPlayerY());
            min = Math.min(min, Math.max(dx, dy));
        }
        return min == Integer.MAX_VALUE ? 12 : min;
    }

    private void updateNpcVisualizationCaches(SimulationState state) {
        npcPredictedPaths.clear();
        npcLineOfSightTiles.clear();

        if (!config.showNpcPaths() && !config.showNpcLosTiles()) {
            return;
        }

        List<Integer> slotsByDistance = new ArrayList<>();
        for (int slot = 0; slot < state.getNpcCount(); slot++) {
            if (state.isNpcActive(slot)) {
                slotsByDistance.add(slot);
            }
        }
        slotsByDistance.sort(Comparator.comparingInt(slot ->
                Math.max(
                        Math.abs(state.getNpcX(slot) - state.getPlayerX()),
                        Math.abs(state.getNpcY(slot) - state.getPlayerY())
                )));

        int limit = Math.max(1, config.maxVisualizedNpcs());
        int maxPathLength = Math.max(1, config.maxNpcPathLength());
        int losRangeCap = Math.max(1, config.npcLosRangeCap());
        int rendered = 0;
        for (int slot : slotsByDistance) {
            if (rendered++ >= limit) {
                break;
            }

            int npcIndex = state.getNpcIndex(slot);
            if (config.showNpcPaths()) {
                npcPredictedPaths.put(npcIndex, engine.predictNpcGreedyPathToPlayer(state, slot, maxPathLength));
            }
            if (config.showNpcLosTiles()) {
                int range = Math.min(Math.max(1, state.getNpcAttackRange(slot)), losRangeCap);
                npcLineOfSightTiles.put(npcIndex, engine.getNpcLineOfSightTiles(state, slot, range));
            }
        }
    }

    private void syncOverlayState() {
        if (config.showSceneOverlay()) {
            overlayManager.add(sceneOverlay);
        } else {
            overlayManager.remove(sceneOverlay);
        }

        if (config.showInfoOverlay()) {
            overlayManager.add(infoOverlay);
        } else {
            overlayManager.remove(infoOverlay);
        }
    }

    private void clearTransientState() {
        lastSnapshot = null;
        rootState = null;
        bestActionState = null;
        lastDecisionResult = null;
        lastExecutableAction = null;
        lastPolicy = null;
        lastSearchMicros = 0L;
        rootThreatCount = 0;
        bestThreatCount = 0;
        rootAttackThreatCount = 0;
        bestAttackThreatCount = 0;
        rootUnprotectedThreatCount = 0;
        bestUnprotectedThreatCount = 0;
        rootRecommendedPrayer = null;
        bestRecommendedPrayer = null;
        lastError = null;
        npcPredictedPaths.clear();
        npcLineOfSightTiles.clear();
    }

    private Map<Integer, SimulationSnapshotService.NpcMetadata> parseNpcCombatOverrides(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Integer, SimulationSnapshotService.NpcMetadata> parsed = new HashMap<>();
        String[] entries = raw.split("[,;]");
        for (String entry : entries) {
            String token = entry.trim();
            if (token.isEmpty()) {
                continue;
            }

            String[] idSplit = token.split("=");
            if (idSplit.length != 2) {
                continue;
            }

            try {
                int npcId = Integer.parseInt(idSplit[0].trim());
                String[] values = idSplit[1].trim().split(":");
                if (values.length < 4) {
                    continue;
                }

                NpcAttackStyle style = NpcAttackStyle.valueOf(values[0].trim().toUpperCase(Locale.ROOT));
                int range = Integer.parseInt(values[1].trim());
                int speed = Integer.parseInt(values[2].trim());
                int maxHit = Integer.parseInt(values[3].trim());
                boolean stopWhenLos = range > 1;

                parsed.put(npcId, new SimulationSnapshotService.NpcMetadata(range, speed, style, maxHit, true, stopWhenLos));
            } catch (Exception ignored) {
                // Keep parsing other entries.
            }
        }
        return parsed;
    }

    private Map<Integer, Integer> parseFoodHealOverrides(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Integer, Integer> parsed = new HashMap<>();
        String[] entries = raw.split("[,;]");
        for (String entry : entries) {
            String token = entry.trim();
            if (token.isEmpty()) {
                continue;
            }

            String[] kv = token.split("=");
            if (kv.length != 2) {
                continue;
            }

            try {
                int itemId = Integer.parseInt(kv[0].trim());
                int heal = Integer.parseInt(kv[1].trim());
                if (itemId >= 0 && heal > 0) {
                    parsed.put(itemId, heal);
                }
            } catch (NumberFormatException ignored) {
                // Keep parsing other entries.
            }
        }
        return parsed;
    }
}
