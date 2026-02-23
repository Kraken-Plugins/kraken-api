package plugins.colosseum;

import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.service.actor.ActorService;
import com.kraken.api.service.prayer.PrayerService;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.HotkeyListener;
import plugins.colosseum.model.spawns.Mob;

import java.util.*;

@Slf4j
@Singleton
@PluginDescriptor(
        name = "Auto Colosseum Prayers",
        description = "Automatically prays for you in the Colosseum.",
        tags = {"auto", "prayers", "colo", "colosseum"}
)
public class AutoColosseumPrayers extends Plugin {
    private static final String CONFIG_GROUP = "autocoloprayers";

    private static final int MANTICORE_MAGE_GRAPHIC = 2681;
    private static final int MANTICORE_RANGE_GRAPHIC = 2683;
    private static final int MANTICORE_MELEE_GRAPHIC = 2685;

    @Inject
    private Context ctx;

    @Inject
    private Client client;

    @Inject
    private PrayerService prayerService;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private AutoColosseumPrayersOverlay statusOverlay;

    @Inject
    private AutoColosseumPrayerQueueOverlay prayerQueueOverlay;

    @Inject
    private KeyManager keyManager;

    @Inject
    private AutoColosseumPrayersConfig config;

    @Inject
    private ColosseumStateTracker tracker;

    private final Map<Integer, TrackedMobState> trackedMobStates = new HashMap<>();
    private final List<PrayerQueueEntry> prayerQueue = new ArrayList<>();
    private static final Map<Integer, Prayer> preprayerMap = Map.ofEntries(
            Map.entry(1, Prayer.PROTECT_FROM_MAGIC),
            Map.entry(2, Prayer.PROTECT_FROM_MISSILES),
            Map.entry(3, Prayer.PROTECT_FROM_MISSILES),
            Map.entry(4, Prayer.PROTECT_FROM_MAGIC),
            Map.entry(5, Prayer.PROTECT_FROM_MISSILES),
            Map.entry(6, Prayer.PROTECT_FROM_MISSILES),
            Map.entry(7, Prayer.PROTECT_FROM_MAGIC),
            Map.entry(8, Prayer.PROTECT_FROM_MAGIC),
            Map.entry(9, Prayer.PROTECT_FROM_MISSILES),
            Map.entry(10, Prayer.PROTECT_FROM_MISSILES),
            Map.entry(11, Prayer.PROTECT_FROM_MAGIC)
    );

    private int tickCounter;
    private long lastTickTime;
    private boolean runtimeEnabled;
    private Prayer activeTargetPrayer;
    private Prayer prePrayPrayer;
    private int prePrayUntilTick = -1;
    private int previousTrackedCount;
    private int lastDetectedWave = -1;

    private final HotkeyListener toggleHotkeyListener = new HotkeyListener(() -> config.toggleHotkey()) {
        @Override
        public void hotkeyPressed() {
            toggleRuntimeState();
        }
    };

    @Provides
    AutoColosseumPrayersConfig provideConfig(final ConfigManager configManager) {
        return configManager.getConfig(AutoColosseumPrayersConfig.class);
    }

    @Override
    public void startUp() {
        ctx.initializePackets();
        runtimeEnabled = config.startEnabled();
        keyManager.registerKeyListener(toggleHotkeyListener);
        syncOverlayState();
        clearTrackingState();
    }

    @Override
    public void shutDown() {
        keyManager.unregisterKeyListener(toggleHotkeyListener);
        overlayManager.remove(statusOverlay);
        overlayManager.remove(prayerQueueOverlay);
        clearTrackingState();
        tickCounter = 0;
        lastTickTime = 0L;
    }

    @Subscribe
    private void onConfigChanged(ConfigChanged event) {
        if (!CONFIG_GROUP.equals(event.getGroup())) {
            return;
        }

        syncOverlayState();
        if (!config.enabled()) {
            clearTrackingState();
        }
    }

    @Subscribe
    private void onGameStateChanged(GameStateChanged event) {
        if (event.getGameState() != GameState.LOGGED_IN) {
            clearTrackingState();
            tickCounter = 0;
            lastTickTime = 0L;
        }
    }

    @Subscribe
    private void onNpcDespawned(NpcDespawned event) {
        if (!config.removeQueuedOnNpcDeath()) {
            return;
        }

        Mob mob = Mob.fromNpc(event.getNpc());
        if (mob == null) {
            return;
        }

        trackedMobStates.remove(event.getNpc().getIndex());
    }

    @Subscribe
    private void onAnimationChanged(AnimationChanged event) {
        if (!isRuntimeEnabled() || !(event.getActor() instanceof NPC)) {
            return;
        }

        NPC npc = (NPC) event.getActor();
        Mob mob = Mob.fromNpc(npc);
        if (mob == null || mob.isManticore()) {
            return;
        }

        TrackedMobState state = trackedMobStates.get(npc.getIndex());
        if (state == null || npc.getAnimation() == -1) {
            return;
        }

        Player localPlayer = client.getLocalPlayer();
        if (localPlayer == null || npc.getInteracting() != localPlayer) {
            return;
        }

        if (!hasLineOfSightToPlayer(npc, mob, localPlayer)) {
            return;
        }

        if (state.getLastAttackAnimationTick() == tickCounter) {
            return;
        }

        int animation = npc.getAnimation();
        Integer knownAnimation = state.getKnownAttackAnimation();
        if (knownAnimation == null) {
            state.setKnownAttackAnimation(animation);
            knownAnimation = animation;
        }

        if (knownAnimation != animation) {
            return;
        }

        state.setSynced(true);
        state.setNextAttackTick(tickCounter + Math.max(1, mob.getAttackSpeed()));
        state.setLastAttackAnimationTick(tickCounter);
    }

    @Subscribe
    private void onGameTick(GameTick event) {
        tickCounter++;
        lastTickTime = System.currentTimeMillis();

        if (!isRuntimeEnabled()) {
            activeTargetPrayer = null;
            prayerQueue.clear();
            return;
        }

        Player localPlayer = client.getLocalPlayer();
        if (localPlayer == null) {
            clearTrackingState();
            return;
        }

        Map<Integer, NPC> colosseumNpcs = collectColosseumNpcs();
        updateWavePrePray(colosseumNpcs);
        updateTrackedStates(colosseumNpcs, localPlayer);
        rebuildPrayerQueue(colosseumNpcs, localPlayer);

        Prayer prayerToActivate = choosePrayerForCurrentTick();
        activeTargetPrayer = prayerToActivate;
        if (prayerToActivate != null) {
            prayerService.activatePrayer(prayerToActivate);
        }
    }

    private void updateTrackedStates(Map<Integer, NPC> colosseumNpcs, Player localPlayer) {
        for (Map.Entry<Integer, NPC> entry : colosseumNpcs.entrySet()) {
            int npcIndex = entry.getKey();
            NPC npc = entry.getValue();
            Mob mob = Mob.fromNpc(npc);
            if (mob == null) {
                continue;
            }

            TrackedMobState state = trackedMobStates.computeIfAbsent(npcIndex, key -> new TrackedMobState(npcIndex, mob));
            state.setLastSeenTick(tickCounter);
            state.setPreviousLineOfSight(state.isInLineOfSight());

            boolean hasLineOfSight = hasLineOfSightToPlayer(npc, mob, localPlayer);
            state.setInLineOfSight(hasLineOfSight);

            if (hasLineOfSight && !state.isPreviousLineOfSight()) {
                onLineOfSightGained(state);
            } else if (!hasLineOfSight && state.isPreviousLineOfSight()) {
                onLineOfSightLost(state);
            }

            if (mob.isManticore()) {
                updateManticoreState(state, npc, hasLineOfSight);
            }
        }

        Iterator<Map.Entry<Integer, TrackedMobState>> iterator = trackedMobStates.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, TrackedMobState> entry = iterator.next();
            if (colosseumNpcs.containsKey(entry.getKey())) {
                continue;
            }

            TrackedMobState state = entry.getValue();
            if (config.removeQueuedOnNpcDeath() || !stateHasFutureAttacks(state)) {
                iterator.remove();
            }
        }
    }

    private void updateManticoreState(TrackedMobState state, NPC npc, boolean hasLineOfSight) {
        ManticoreAttackStyle firstStyle = currentManticoreSpotAnimation(npc);
        if (firstStyle != null && !state.isCharging()) {
            state.setCharging(true);
            state.setChargeStartTick(tickCounter);
            state.setFirstManticoreStyle(firstStyle);
            state.setChargeInterrupted(false);
            state.setFirstVolleyTick(tickCounter + Math.max(1, config.manticoreChargeTicks()));
            state.setFirstVolleyAuto(config.autoFirstManticoreVolley() && hasLineOfSight);
            state.setSynced(false);
            state.setNextVolleyTick(-1);
        }

        if (!state.isCharging()) {
            return;
        }

        if (!hasLineOfSight) {
            state.setChargeInterrupted(true);
        }

        if (tickCounter < state.getFirstVolleyTick()) {
            return;
        }

        state.setCharging(false);
        boolean autoFirstVolley = state.isFirstVolleyAuto() && !state.isChargeInterrupted();
        state.setFirstVolleyAuto(autoFirstVolley);
        state.setSynced(true);
        state.setNextVolleyTick(state.getFirstVolleyTick() + Math.max(1, state.getMob().getAttackSpeed()));
    }

    private void rebuildPrayerQueue(Map<Integer, NPC> currentNpcs, Player localPlayer) {
        prayerQueue.clear();
        int lookahead = Math.max(1, config.queueLookaheadTicks());

        for (TrackedMobState state : trackedMobStates.values()) {
            state.setLastQueuedTick(-1);
            if (state.getMob().isManticore()) {
                addManticoreQueueEntries(state, lookahead);
            } else {
                addStandardQueueEntries(state, lookahead);
            }
        }

        if (config.prayJaguarOnPath()) {
            addJaguarPriorityEntries(currentNpcs, localPlayer, lookahead);
        }

        prayerQueue.sort(
                Comparator.comparingInt(PrayerQueueEntry::getTick)
                        .thenComparing(Comparator.comparingInt(PrayerQueueEntry::getMaxHit).reversed())
                        .thenComparingInt(PrayerQueueEntry::getNpcIndex)
        );
    }

    private void addStandardQueueEntries(TrackedMobState state, int lookahead) {
        Mob mob = state.getMob();
        if (!state.isSynced() || mob.getPrayer() == null) {
            return;
        }

        if (config.cancelQueuedOnLosBreak() && !state.isInLineOfSight()) {
            return;
        }

        int attackSpeed = Math.max(1, mob.getAttackSpeed());
        int nextAttackTick = state.getNextAttackTick();
        if (nextAttackTick < 0) {
            return;
        }

        while (nextAttackTick < tickCounter) {
            nextAttackTick += attackSpeed;
        }

        state.setNextAttackTick(nextAttackTick);
        for (int queuedTick = nextAttackTick; queuedTick <= tickCounter + lookahead; queuedTick += attackSpeed) {
            prayerQueue.add(new PrayerQueueEntry(
                    queuedTick,
                    state.getNpcIndex(),
                    mob,
                    mob.getPrayer(),
                    mob.getMaxHit(),
                    false
            ));
            state.setLastQueuedTick(queuedTick);
        }
    }

    private void addManticoreQueueEntries(TrackedMobState state, int lookahead) {
        if (state.getFirstManticoreStyle() == null) {
            return;
        }

        if (config.cancelQueuedOnLosBreak() && !state.isInLineOfSight() && !state.isCharging()) {
            return;
        }

        List<ManticoreAttackStyle> sequence = ManticoreAttackStyle.sequenceForFirst(state.getFirstManticoreStyle());
        if (sequence.isEmpty()) {
            return;
        }

        if (state.isFirstVolleyAuto() && state.getFirstVolleyTick() >= tickCounter) {
            queueManticoreVolley(state, sequence, state.getFirstVolleyTick(), lookahead);
        }

        if (!state.isSynced() || state.getNextVolleyTick() < 0) {
            return;
        }

        int attackSpeed = Math.max(1, state.getMob().getAttackSpeed());
        int volleyTick = state.getNextVolleyTick();
        while (volleyTick + sequence.size() - 1 < tickCounter) {
            volleyTick += attackSpeed;
        }

        state.setNextVolleyTick(volleyTick);
        for (int queuedTick = volleyTick; queuedTick <= tickCounter + lookahead; queuedTick += attackSpeed) {
            queueManticoreVolley(state, sequence, queuedTick, lookahead);
        }
    }

    private void queueManticoreVolley(
            TrackedMobState state,
            List<ManticoreAttackStyle> sequence,
            int volleyStartTick,
            int lookahead
    ) {
        int furthest = -1;
        for (int styleTick = 0; styleTick < sequence.size(); styleTick++) {
            int queuedTick = volleyStartTick + styleTick;
            if (queuedTick < tickCounter || queuedTick > tickCounter + lookahead) {
                continue;
            }

            Prayer prayer = sequence.get(styleTick).getProtectionPrayer();
            prayerQueue.add(new PrayerQueueEntry(
                    queuedTick,
                    state.getNpcIndex(),
                    state.getMob(),
                    prayer,
                    state.getMob().getMaxHit(),
                    false
            ));
            furthest = Math.max(furthest, queuedTick);
        }

        if (furthest > -1) {
            state.setLastQueuedTick(Math.max(state.getLastQueuedTick(), furthest));
        }
    }

    private void addJaguarPriorityEntries(Map<Integer, NPC> currentNpcs, Player localPlayer, int lookahead) {
        for (NPC npc : currentNpcs.values()) {
            Mob mob = Mob.fromNpc(npc);
            if (mob == null || !mob.isJaguarWarrior()) {
                continue;
            }

            TrackedMobState state = trackedMobStates.get(npc.getIndex());
            if (state == null) {
                continue;
            }

            int predictedAttackTick = predictJaguarAttackTick(npc, localPlayer, state);
            if (predictedAttackTick < tickCounter || predictedAttackTick > tickCounter + lookahead) {
                continue;
            }

            prayerQueue.add(new PrayerQueueEntry(
                    predictedAttackTick,
                    npc.getIndex(),
                    mob,
                    Prayer.PROTECT_FROM_MELEE,
                    mob.getMaxHit() + 1000,
                    true
            ));
            state.setLastQueuedTick(Math.max(state.getLastQueuedTick(), predictedAttackTick));
        }
    }

    private int predictJaguarAttackTick(NPC npc, Player localPlayer, TrackedMobState state) {
        if (state.isSynced() && state.getNextAttackTick() >= 0) {
            int attackSpeed = Math.max(1, state.getMob().getAttackSpeed());
            int tick = state.getNextAttackTick();
            while (tick < tickCounter) {
                tick += attackSpeed;
            }
            return tick;
        }

        List<WorldPoint> pathToPlayer = ActorService.getActorPath(npc, localPlayer);
        if (pathToPlayer == null) {
            return -1;
        }

        if (pathToPlayer.isEmpty()) {
            return state.isInLineOfSight() ? tickCounter : -1;
        }

        int stepsUntilAdjacent = Math.max(0, pathToPlayer.size() - 1);
        return tickCounter + stepsUntilAdjacent + 1;
    }

    private Prayer choosePrayerForCurrentTick() {
        Prayer selectedPrayer = null;
        int highestMaxHit = Integer.MIN_VALUE;

        for (PrayerQueueEntry queueEntry : prayerQueue) {
            if (queueEntry.getTick() != tickCounter) {
                continue;
            }

            if (queueEntry.isJaguarPriority()) {
                return Prayer.PROTECT_FROM_MELEE;
            }

            if (queueEntry.getMaxHit() > highestMaxHit) {
                highestMaxHit = queueEntry.getMaxHit();
                selectedPrayer = queueEntry.getPrayer();
            }
        }

        if (selectedPrayer != null) {
            return selectedPrayer;
        }

        if (prePrayPrayer != null && tickCounter <= prePrayUntilTick) {
            return prePrayPrayer;
        }

        return null;
    }

    private Map<Integer, NPC> collectColosseumNpcs() {
        Map<Integer, NPC> result = new HashMap<>();
        if (client.getTopLevelWorldView() == null) {
            return result;
        }

        for (NPC npc : client.getTopLevelWorldView().npcs()) {
            if (npc == null || npc.isDead()) {
                continue;
            }

            Mob mob = Mob.fromNpc(npc);
            if (mob == null) {
                continue;
            }

            result.put(npc.getIndex(), npc);
        }

        return result;
    }

    private void updateWavePrePray(Map<Integer, NPC> currentNpcs) {
        if (!config.enableWavePrePray()) {
            prePrayPrayer = null;
            prePrayUntilTick = -1;
            previousTrackedCount = currentNpcs.size();
            return;
        }

        int wave = tracker.getCurrentState().getWaveNumber();
        if (wave > 0 && wave != lastDetectedWave) {
            lastDetectedWave = wave;
            beginPrePray(resolveWavePrePrayer(wave));
        } else if (wave <= 0 && previousTrackedCount == 0 && !currentNpcs.isEmpty()) {
            beginPrePray(resolveWavePrePrayer(0));
        }

        if (tickCounter > prePrayUntilTick) {
            prePrayPrayer = null;
        }

        previousTrackedCount = currentNpcs.size();
    }

    private Prayer resolveWavePrePrayer(int wave) {
        if(!preprayerMap.containsKey(wave)) {
            log.error("Unknown wave {}, defaulting to mage pre-prayer", wave);
            return Prayer.PROTECT_FROM_MAGIC;
        }

        return preprayerMap.get(wave);
    }

    private void beginPrePray(Prayer prayer) {
        if (prayer == null) {
            return;
        }

        prePrayPrayer = prayer;
        prePrayUntilTick = tickCounter + Math.max(1, config.prePrayDurationTicks()) - 1;
    }

    private boolean hasLineOfSightToPlayer(NPC npc, Mob mob, Player player) {
        if (npc == null || mob == null || player == null) {
            return false;
        }

        WorldPoint playerPoint = player.getWorldLocation();
        if (playerPoint == null || npc.getWorldLocation() == null) {
            return false;
        }

        List<WorldPoint> visibleTiles = ActorService.getLineOfSightTiles(npc, Math.max(1, mob.getAttackRange()));
        return visibleTiles.contains(playerPoint);
    }

    private void onLineOfSightGained(TrackedMobState state) {
        if (state.getMob().isManticore()) {
            return;
        }

        if (config.cancelQueuedOnLosBreak()) {
            state.setSynced(false);
            state.setNextAttackTick(-1);
        }
    }

    private void onLineOfSightLost(TrackedMobState state) {
        if (state.getMob().isManticore() && state.isCharging()) {
            state.setChargeInterrupted(true);
            state.setFirstVolleyAuto(false);
            return;
        }

        if (!config.cancelQueuedOnLosBreak()) {
            return;
        }

        clearQueuedPrediction(state);
    }

    private void clearQueuedPrediction(TrackedMobState state) {
        state.setSynced(false);
        state.setNextAttackTick(-1);
        state.setLastQueuedTick(-1);

        if (!state.getMob().isManticore()) {
            return;
        }

        state.setFirstVolleyTick(-1);
        state.setFirstVolleyAuto(false);
        state.setNextVolleyTick(-1);
    }

    private boolean stateHasFutureAttacks(TrackedMobState state) {
        if (state.getLastQueuedTick() >= tickCounter) {
            return true;
        }
        if (!state.getMob().isManticore()) {
            return state.getNextAttackTick() >= tickCounter;
        }
        return (state.isFirstVolleyAuto() && state.getFirstVolleyTick() >= tickCounter)
                || state.getNextVolleyTick() >= tickCounter
                || state.isCharging();
    }

    private ManticoreAttackStyle currentManticoreSpotAnimation(NPC npc) {
        if (npc == null) {
            return null;
        }
        if (npc.hasSpotAnim(MANTICORE_MAGE_GRAPHIC)) {
            return ManticoreAttackStyle.MAGE;
        }
        if (npc.hasSpotAnim(MANTICORE_RANGE_GRAPHIC)) {
            return ManticoreAttackStyle.RANGE;
        }
        if (npc.hasSpotAnim(MANTICORE_MELEE_GRAPHIC)) {
            return ManticoreAttackStyle.MELEE;
        }
        return null;
    }

    private void syncOverlayState() {
        if (config.showStatusOverlay()) {
            overlayManager.add(statusOverlay);
        } else {
            overlayManager.remove(statusOverlay);
        }

        if (config.showPrayerQueueOnPrayerTab()) {
            overlayManager.add(prayerQueueOverlay);
        } else {
            overlayManager.remove(prayerQueueOverlay);
        }
    }

    private void clearTrackingState() {
        trackedMobStates.clear();
        prayerQueue.clear();
        activeTargetPrayer = null;
        prePrayPrayer = null;
        prePrayUntilTick = -1;
        previousTrackedCount = 0;
        lastDetectedWave = -1;
    }

    private void toggleRuntimeState() {
        if (!config.enabled()) {
            return;
        }

        runtimeEnabled = !runtimeEnabled;
        clearTrackingState();
        log.info("Auto Colosseum Prayers runtime {}", runtimeEnabled ? "enabled" : "disabled");
    }

    boolean isRuntimeEnabled() {
        return config.enabled() && runtimeEnabled;
    }

    Prayer getActiveTargetPrayer() {
        return activeTargetPrayer;
    }

    Prayer getPrePrayPrayer() {
        return prePrayPrayer;
    }

    Prayer getActiveProtectionPrayer() {
        if (client.isPrayerActive(Prayer.PROTECT_FROM_MELEE)) {
            return Prayer.PROTECT_FROM_MELEE;
        }
        if (client.isPrayerActive(Prayer.PROTECT_FROM_MISSILES)) {
            return Prayer.PROTECT_FROM_MISSILES;
        }
        if (client.isPrayerActive(Prayer.PROTECT_FROM_MAGIC)) {
            return Prayer.PROTECT_FROM_MAGIC;
        }
        return null;
    }

    int getRemainingPrePrayTicks() {
        if (prePrayPrayer == null || prePrayUntilTick < tickCounter) {
            return 0;
        }
        return prePrayUntilTick - tickCounter + 1;
    }

    int getCurrentTick() {
        return tickCounter;
    }

    long getLastTickTime() {
        return lastTickTime;
    }

    List<PrayerQueueEntry> getPrayerQueueSnapshot() {
        return new ArrayList<>(prayerQueue);
    }
}
