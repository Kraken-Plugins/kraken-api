package plugins.colosseum;

import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.service.actor.ActorService;
import com.kraken.api.service.prayer.PrayerService;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Prayer;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcDespawned;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.HotkeyListener;
import plugins.colosseum.model.ColosseumState;
import plugins.colosseum.model.ColosseumStateChanged;
import plugins.colosseum.model.spawns.Mob;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Singleton
@PluginDescriptor(
        name = "Auto Colosseum Prayers",
        description = "Automatically prays for you in the Colosseum.",
        tags = {"auto", "prayers", "colo", "colosseum"}
)
public class AutoColosseumPrayersPlugin extends Plugin {
    private static final String CONFIG_GROUP = "autocoloprayers";

    private static final int MANTICORE_MAGE_GRAPHIC = 2681;
    private static final int MANTICORE_RANGE_GRAPHIC = 2683;
    private static final int MANTICORE_MELEE_GRAPHIC = 2685;

    private static final Map<Integer, Prayer> WAVE_PRE_PRAYER_MAP = Map.ofEntries(
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
    private AutoColosseumNpcDebugOverlay npcDebugOverlay;

    @Inject
    private KeyManager keyManager;

    @Inject
    private AutoColosseumPrayersConfig config;

    @Inject
    private ColosseumStateTracker tracker;

    private final Map<Integer, TrackedMobState> trackedMobStates = new HashMap<>();
    private final List<PrayerQueueEntry> prayerQueue = new ArrayList<>();

    private long lastTickTime;
    private boolean runtimeEnabled;
    private Prayer activeTargetPrayer;
    private Prayer prePrayPrayer;
    private int prePrayUntilTick = -1;
    private int lastWaveNumberStarted = -1;
    private int lastWaveStartTick = -1;
    private Prayer lastAutoActivatedPrayer;
    private int lastAutoActivatedTick = -1;

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
    protected void startUp() {
        ctx.initializePackets();
        runtimeEnabled = config.startEnabled();
        keyManager.registerKeyListener(toggleHotkeyListener);
        syncOverlayState();
        clearTrackingState();
    }

    @Override
    protected void shutDown() {
        keyManager.unregisterKeyListener(toggleHotkeyListener);
        overlayManager.remove(statusOverlay);
        overlayManager.remove(prayerQueueOverlay);
        overlayManager.remove(npcDebugOverlay);
        clearTrackingState();
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
    private void onColosseumStateChanged(ColosseumStateChanged event) {
        if (!config.enableWavePrePray()) {
            clearPrePrayState();
            return;
        }

        ColosseumState next = event.getNewState();
        ColosseumState previous = event.getPreviousState();
        if (!next.isInColosseum() || next.isInLobby()) {
            clearPrePrayState();
            return;
        }

        boolean waveJustStarted = next.isWaveStarted()
                && (!previous.isWaveStarted() || next.getWaveNumber() != previous.getWaveNumber());

        if (!waveJustStarted) {
            return;
        }

        int waveNumber = next.getWaveNumber();
        int waveStartTick = tracker.getWaveStartTick();
        if (waveNumber <= 0 || waveStartTick <= 0) {
            return;
        }

        if (waveNumber == lastWaveNumberStarted && waveStartTick == lastWaveStartTick) {
            return;
        }

        lastWaveNumberStarted = waveNumber;
        lastWaveStartTick = waveStartTick;
        beginPrePray(waveNumber, waveStartTick);
    }

    @Subscribe
    private void onGameStateChanged(GameStateChanged event) {
        if (event.getGameState() == GameState.LOGGED_IN && !ctx.isPacketsLoaded()) {
            ctx.initializePackets();
        }

        if (event.getGameState() != GameState.LOGGED_IN) {
            clearTrackingState();
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

        int currentTick = currentTick();
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

        if (state.getLastAttackAnimationTick() == currentTick) {
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
        state.setNextAttackTick(currentTick + Math.max(1, mob.getAttackSpeed()));
        state.setLastAttackAnimationTick(currentTick);
    }

    @Subscribe
    private void onGameTick(GameTick event) {
        int currentTick = currentTick();
        lastTickTime = System.currentTimeMillis();

        if (!ctx.isPacketsLoaded()) {
            ctx.initializePackets();
        }

        if (!isRuntimeEnabled()) {
            activeTargetPrayer = null;
            prayerQueue.clear();
            return;
        }

        if (!isInsideColosseum()) {
            clearTrackingState();
            return;
        }

        Player localPlayer = client.getLocalPlayer();
        if (localPlayer == null) {
            clearTrackingState();
            return;
        }

        maintainPrePrayState(currentTick);

        Map<Integer, NPC> colosseumNpcs = collectColosseumNpcs();
        updateTrackedStates(colosseumNpcs, localPlayer, currentTick);
        rebuildPrayerQueue(colosseumNpcs, localPlayer, currentTick);

        Prayer prayerToActivate = choosePrayerForCurrentTick(currentTick);
        applyPrayerDecision(prayerToActivate, currentTick);
    }

    private void updateTrackedStates(Map<Integer, NPC> colosseumNpcs, Player localPlayer, int currentTick) {
        for (Map.Entry<Integer, NPC> entry : colosseumNpcs.entrySet()) {
            int npcIndex = entry.getKey();
            NPC npc = entry.getValue();
            Mob mob = Mob.fromNpc(npc);
            if (mob == null) {
                continue;
            }

            TrackedMobState state = trackedMobStates.computeIfAbsent(npcIndex, key -> new TrackedMobState(npcIndex, mob));
            state.setLastSeenTick(currentTick);
            state.setPreviousLineOfSight(state.isInLineOfSight());

            boolean hasLineOfSight = hasLineOfSightToPlayer(npc, mob, localPlayer);
            state.setInLineOfSight(hasLineOfSight);

            if (hasLineOfSight && !state.isPreviousLineOfSight()) {
                onLineOfSightGained(state);
            } else if (!hasLineOfSight && state.isPreviousLineOfSight()) {
                onLineOfSightLost(state);
            }

            if (mob.isManticore()) {
                updateManticoreState(state, npc, hasLineOfSight, currentTick);
            }
        }

        Iterator<Map.Entry<Integer, TrackedMobState>> iterator = trackedMobStates.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, TrackedMobState> entry = iterator.next();
            if (colosseumNpcs.containsKey(entry.getKey())) {
                continue;
            }

            TrackedMobState state = entry.getValue();
            if (config.removeQueuedOnNpcDeath() || !stateHasFutureAttacks(state, currentTick)) {
                iterator.remove();
            }
        }
    }

    private void updateManticoreState(TrackedMobState state, NPC npc, boolean hasLineOfSight, int currentTick) {
        ManticoreAttackStyle firstStyle = currentManticoreSpotAnimation(npc);
        if (firstStyle != null && !state.isCharging()) {
            state.setCharging(true);
            state.setChargeStartTick(currentTick);
            state.setFirstManticoreStyle(firstStyle);
            state.setChargeInterrupted(false);
            state.setFirstVolleyTick(currentTick + Math.max(1, 10));
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

        if (currentTick < state.getFirstVolleyTick()) {
            return;
        }

        state.setCharging(false);
        boolean autoFirstVolley = state.isFirstVolleyAuto() && !state.isChargeInterrupted();
        state.setFirstVolleyAuto(autoFirstVolley);
        state.setSynced(true);
        state.setNextVolleyTick(state.getFirstVolleyTick() + Math.max(1, state.getMob().getAttackSpeed()));
    }

    private void rebuildPrayerQueue(Map<Integer, NPC> currentNpcs, Player localPlayer, int currentTick) {
        prayerQueue.clear();
        int lookahead = Math.max(1, config.queueLookaheadTicks());

        for (TrackedMobState state : trackedMobStates.values()) {
            state.setLastQueuedTick(-1);
            if (state.getMob().isManticore()) {
                addManticoreQueueEntries(state, lookahead, currentTick);
            } else {
                addStandardQueueEntries(state, lookahead, currentTick);
            }
        }

        if (config.prayJaguarOnPath()) {
            addJaguarPriorityEntries(currentNpcs, localPlayer, lookahead, currentTick);
        }

        prayerQueue.sort(
                Comparator.comparingInt(PrayerQueueEntry::getTick)
                        .thenComparing(Comparator.comparingInt(PrayerQueueEntry::getMaxHit).reversed())
                        .thenComparingInt(PrayerQueueEntry::getNpcIndex)
        );
    }

    private void addStandardQueueEntries(TrackedMobState state, int lookahead, int currentTick) {
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

        while (nextAttackTick < currentTick) {
            nextAttackTick += attackSpeed;
        }

        state.setNextAttackTick(nextAttackTick);
        for (int queuedTick = nextAttackTick; queuedTick <= currentTick + lookahead; queuedTick += attackSpeed) {
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

    private void addManticoreQueueEntries(TrackedMobState state, int lookahead, int currentTick) {
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

        if (state.isFirstVolleyAuto() && state.getFirstVolleyTick() >= currentTick) {
            queueManticoreVolley(state, sequence, state.getFirstVolleyTick(), lookahead, currentTick);
        }

        if (!state.isSynced() || state.getNextVolleyTick() < 0) {
            return;
        }

        int attackSpeed = Math.max(1, state.getMob().getAttackSpeed());
        int volleyTick = state.getNextVolleyTick();
        while (volleyTick + sequence.size() - 1 < currentTick) {
            volleyTick += attackSpeed;
        }

        state.setNextVolleyTick(volleyTick);
        for (int queuedTick = volleyTick; queuedTick <= currentTick + lookahead; queuedTick += attackSpeed) {
            queueManticoreVolley(state, sequence, queuedTick, lookahead, currentTick);
        }
    }

    private void queueManticoreVolley(
            TrackedMobState state,
            List<ManticoreAttackStyle> sequence,
            int volleyStartTick,
            int lookahead,
            int currentTick
    ) {
        int furthest = -1;
        for (int styleTick = 0; styleTick < sequence.size(); styleTick++) {
            int queuedTick = volleyStartTick + styleTick;
            if (queuedTick < currentTick || queuedTick > currentTick + lookahead) {
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

    private void addJaguarPriorityEntries(Map<Integer, NPC> currentNpcs, Player localPlayer, int lookahead, int currentTick) {
        for (NPC npc : currentNpcs.values()) {
            Mob mob = Mob.fromNpc(npc);
            if (mob == null || !mob.isJaguarWarrior()) {
                continue;
            }

            TrackedMobState state = trackedMobStates.get(npc.getIndex());
            if (state == null) {
                continue;
            }

            int predictedAttackTick = predictJaguarAttackTick(npc, localPlayer, state, currentTick);
            if (predictedAttackTick < currentTick || predictedAttackTick > currentTick + lookahead) {
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

    private int predictJaguarAttackTick(NPC npc, Player localPlayer, TrackedMobState state, int currentTick) {
        if (state.isSynced() && state.getNextAttackTick() >= 0) {
            int attackSpeed = Math.max(1, state.getMob().getAttackSpeed());
            int tick = state.getNextAttackTick();
            while (tick < currentTick) {
                tick += attackSpeed;
            }
            return tick;
        }

        List<WorldPoint> pathToPlayer = ActorService.getActorPath(npc, localPlayer);
        if (pathToPlayer == null) {
            return -1;
        }

        if (pathToPlayer.isEmpty()) {
            return state.isInLineOfSight() ? currentTick : -1;
        }

        int stepsUntilAdjacent = Math.max(0, pathToPlayer.size() - 1);
        return currentTick + stepsUntilAdjacent + 1;
    }

    private Prayer choosePrayerForCurrentTick(int currentTick) {
        Prayer currentTickPrayer = bestPrayerForTick(currentTick);
        if (currentTickPrayer != null) {
            return currentTickPrayer;
        }

        // Pre-switch one tick early so prayer is up before the next incoming hit.
        Prayer nextTickPrayer = bestPrayerForTick(currentTick + 1);
        if (nextTickPrayer != null) {
            return nextTickPrayer;
        }

        if (prePrayPrayer != null && currentTick <= prePrayUntilTick) {
            return prePrayPrayer;
        }

        return null;
    }

    private Prayer bestPrayerForTick(int tick) {
        Prayer selectedPrayer = null;
        int highestMaxHit = Integer.MIN_VALUE;

        for (PrayerQueueEntry queueEntry : prayerQueue) {
            if (queueEntry.getTick() != tick) {
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

        return selectedPrayer;
    }

    private void applyPrayerDecision(Prayer prayerToActivate, int currentTick) {
        activeTargetPrayer = prayerToActivate;

        if (prayerToActivate != null) {
            boolean toggled = prayerService.toggle(prayerToActivate, true);
            if (toggled || client.isPrayerActive(prayerToActivate)) {
                lastAutoActivatedPrayer = prayerToActivate;
                lastAutoActivatedTick = currentTick;
            }
            return;
        }

        if (!config.enableOneTickFlick()) {
            return;
        }

        if (lastAutoActivatedPrayer == null || lastAutoActivatedTick != currentTick - 1) {
            return;
        }

        if (!isSafeToOneTickFlick(currentTick)) {
            return;
        }

        if (client.isPrayerActive(lastAutoActivatedPrayer)) {
            prayerService.deactivatePrayer(lastAutoActivatedPrayer);
        }
    }

    private boolean isSafeToOneTickFlick(int currentTick) {
        if (prePrayPrayer != null && currentTick <= prePrayUntilTick) {
            return false;
        }

        int maxThreatCount = Math.max(1, config.oneTickSafeNpcCount());
        Set<Integer> threateningNpcs = new HashSet<>();

        for (PrayerQueueEntry queueEntry : prayerQueue) {
            if (queueEntry.isJaguarPriority() && queueEntry.getTick() <= currentTick + 1) {
                return false;
            }

            if (queueEntry.getTick() < currentTick || queueEntry.getTick() > currentTick + 1) {
                continue;
            }

            threateningNpcs.add(queueEntry.getNpcIndex());
            if (threateningNpcs.size() > maxThreatCount) {
                return false;
            }
        }

        return true;
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

    private Prayer resolveWavePrePrayer(int wave) {
        if (!WAVE_PRE_PRAYER_MAP.containsKey(wave)) {
            log.debug("Unknown wave {} for pre-prayer", wave);
            return null;
        }

        return WAVE_PRE_PRAYER_MAP.get(wave);
    }

    private void beginPrePray(int waveNumber, int waveStartTick) {
        if (!isRuntimeEnabled()) {
            return;
        }

        Prayer prayer = resolveWavePrePrayer(waveNumber);
        if (prayer == null) {
            return;
        }

        int startTick = Math.max(currentTick(), waveStartTick);
        prePrayPrayer = prayer;
        prePrayUntilTick = startTick + Math.max(1, config.prePrayDurationTicks()) - 1;
    }

    private void maintainPrePrayState(int currentTick) {
        if (!isInsideColosseum()) {
            clearPrePrayState();
            return;
        }

        if (prePrayPrayer != null && currentTick > prePrayUntilTick) {
            clearPrePrayState();
        }
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

    private boolean stateHasFutureAttacks(TrackedMobState state, int currentTick) {
        if (state.getLastQueuedTick() >= currentTick) {
            return true;
        }
        if (!state.getMob().isManticore()) {
            return state.getNextAttackTick() >= currentTick;
        }
        return (state.isFirstVolleyAuto() && state.getFirstVolleyTick() >= currentTick)
                || state.getNextVolleyTick() >= currentTick
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

        if (config.showNpcLineOfSightDebug() || config.showNpcPathingDebug()) {
            overlayManager.add(npcDebugOverlay);
        } else {
            overlayManager.remove(npcDebugOverlay);
        }
    }

    private void clearTrackingState() {
        trackedMobStates.clear();
        prayerQueue.clear();
        activeTargetPrayer = null;
        clearPrePrayState();
        lastWaveNumberStarted = -1;
        lastWaveStartTick = -1;
        lastAutoActivatedPrayer = null;
        lastAutoActivatedTick = -1;
    }

    private void clearPrePrayState() {
        prePrayPrayer = null;
        prePrayUntilTick = -1;
    }

    private void toggleRuntimeState() {
        if (!config.enabled()) {
            return;
        }

        runtimeEnabled = !runtimeEnabled;
        clearTrackingState();
        log.info("Auto Colosseum Prayers runtime {}", runtimeEnabled ? "enabled" : "disabled");
    }

    private boolean isInsideColosseum() {
        ColosseumState state = tracker.getCurrentState();
        return state.isInColosseum() && !state.isInLobby();
    }

    private int currentTick() {
        return client.getTickCount();
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
        int currentTick = currentTick();
        if (prePrayPrayer == null || prePrayUntilTick < currentTick) {
            return 0;
        }
        return prePrayUntilTick - currentTick + 1;
    }

    int getTickCounter() {
        return currentTick();
    }

    int getCurrentTick() {
        return currentTick();
    }

    long getLastTickTime() {
        return lastTickTime;
    }

    List<PrayerQueueEntry> getPrayerQueueSnapshot() {
        return new ArrayList<>(prayerQueue);
    }

    int getCurrentWaveNumber() {
        return tracker.getCurrentState().getWaveNumber();
    }

    int getWaveStartTick() {
        return tracker.getWaveStartTick();
    }

    boolean isWaveStarted() {
        return tracker.getCurrentState().isWaveStarted();
    }

    boolean isInColosseum() {
        return tracker.getCurrentState().isInColosseum();
    }

    boolean isInLobby() {
        return tracker.getCurrentState().isInLobby();
    }

    int getTrackedMobCount() {
        return trackedMobStates.size();
    }

    int getQueueSize() {
        return prayerQueue.size();
    }

    int getPrePrayUntilTick() {
        return prePrayUntilTick;
    }

    boolean isPacketsLoaded() {
        return ctx.isPacketsLoaded();
    }
}
