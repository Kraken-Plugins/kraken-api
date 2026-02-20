package com.kraken.api.simulation.plugin;

import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.simulation.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GameState;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import java.util.*;
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
    private long lastSearchMicros;

    @Getter
    private int rootThreatCount;

    @Getter
    private int bestThreatCount;

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
        int radius = Math.max(1, config.snapshotNpcRadius());
        lastSnapshot = SimulationSnapshotService.capture(radius);
        rootState = lastSnapshot.createState();

        List<SimulationAction> candidates = generateCandidateActions(rootState);
        if (candidates.isEmpty()) {
            candidates = Collections.singletonList(SimulationAction.WAIT);
        }

        DecisionTreeSearch search = new DecisionTreeSearch(engine, Math.max(64, config.maxSearchNodes()));
        long startedNanos = System.nanoTime();
        lastDecisionResult = search.search(
                rootState,
                Math.max(1, config.searchDepth()),
                (state, depth) -> generateCandidateActions(state),
                this::evaluateState
        );
        lastSearchMicros = (System.nanoTime() - startedNanos) / 1000L;

        bestActionState = engine.simulateTickCopy(rootState, lastDecisionResult.getBestAction());
        rootThreatCount = engine.countNpcsWithLineOfSightToPlayer(rootState);
        bestThreatCount = engine.countNpcsWithLineOfSightToPlayer(bestActionState);

        updateNpcVisualizationCaches(rootState);
        lastExecutableAction = decisionAdapter.adapt(
                lastDecisionResult,
                rootState,
                config.executeNpcInteraction() ? config.interactionAction() : null,
                config.interactionDistance()
        );

        if (config.autoExecuteBestAction()) {
            decisionAdapter.execute(lastExecutableAction);
        }
    }

    private List<SimulationAction> generateCandidateActions(SimulationState state) {
        LinkedHashSet<SimulationAction> all = new LinkedHashSet<>(SimulationAction.standardWalkActions());
        if (config.includeRunActions()) {
            all.addAll(RUN_ACTIONS);
        }

        return all.stream()
                .filter(action -> engine.canApplyPlayerAction(state, action))
                .collect(Collectors.toList());
    }

    private double evaluateState(SimulationState state) {
        int losThreats = engine.countNpcsWithLineOfSightToPlayer(state);
        int nearestNpcDistance = nearestNpcChebyshevDistance(state);

        double score = 0.0;
        score -= (losThreats * 30.0);
        score += Math.min(nearestNpcDistance, 12) * 2.0;

        if (engine.isPlayerTileSafe(state)) {
            score += 40.0;
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
        lastSearchMicros = 0L;
        rootThreatCount = 0;
        bestThreatCount = 0;
        lastError = null;
        npcPredictedPaths.clear();
        npcLineOfSightTiles.clear();
    }
}
