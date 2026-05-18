package plugins.colosseum;

import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.service.actor.ActorService;
import com.kraken.api.service.prayer.PrayerService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.ProjectileMoved;
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
import plugins.colosseum.model.PrayerQueueEntry;
import plugins.colosseum.model.TrackedMobState;
import plugins.colosseum.model.spawns.Mob;
import plugins.colosseum.overlay.AutoColosseumNpcDebugOverlay;
import plugins.colosseum.overlay.AutoColosseumPrayerQueueOverlay;
import plugins.colosseum.overlay.AutoColosseumPrayersOverlay;

import java.util.*;

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
    private static final int MANTICORE_SPOT_TO_HIT_TICKS = 5;
    private static final int MANTICORE_READY_DELAY_TICKS = 5;
    private static final int MANTICORE_VOLLEY_SIZE = 3;

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

    @Getter
    private final Map<Integer, TrackedMobState> trackedMobStates = new HashMap<>();

    @Getter
    private final List<PrayerQueueEntry> prayerQueue = new ArrayList<>();

    @Getter
    private long lastTickTime = -1;

    @Getter
    private Prayer activeTargetPrayer;

    @Getter
    private Prayer prePrayPrayer;

    @Getter
    private boolean oneTickFlickEnabled;

    @Getter
    private boolean runtimeEnabled;

    private int lastWaveNumberStarted = -1;
    private int lastWaveStartTick = -1;
    private Prayer lastAutoActivatedPrayer;
    private int lastAutoActivatedTick = -1;
    private boolean prePrayPending;


    private final HotkeyListener toggleHotkeyListener = new HotkeyListener(() -> config.toggleHotkey()) {
        @Override
        public void hotkeyPressed() {
            toggleRuntimeState();
        }
    };

    private final HotkeyListener oneTickFlickHotkeyListener = new HotkeyListener(() -> config.oneTickFlickHotkey()) {
        @Override
        public void hotkeyPressed() {
            oneTickFlickEnabled = !oneTickFlickEnabled;
            log.info("Auto Colosseum Prayers 1-tick flick {}", oneTickFlickEnabled ? "enabled" : "disabled");
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
        oneTickFlickEnabled = false;
        keyManager.registerKeyListener(toggleHotkeyListener);
        keyManager.registerKeyListener(oneTickFlickHotkeyListener);
        syncOverlayState();
        clearTrackingState();
    }

    @Override
    protected void shutDown() {
        keyManager.unregisterKeyListener(toggleHotkeyListener);
        keyManager.unregisterKeyListener(oneTickFlickHotkeyListener);
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
        beginPrePray(waveNumber);
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

        int currentTick = client.getTickCount();
        NPC npc = (NPC) event.getActor();
        Mob mob = Mob.fromNpc(npc);
        if (mob == null || mob.isManticore()) {
            return;
        }

        if (npc.getAnimation() == -1) {
            return;
        }

        Player localPlayer = client.getLocalPlayer();
        if (localPlayer == null) {
            return;
        }

        Map<Integer, NPC> colosseumNpcs = collectColosseumNpcs();
        boolean hasLineOfSight = hasLineOfSightToPlayer(npc, mob, localPlayer, colosseumNpcs);
        if (!hasLineOfSight && npc.getInteracting() != localPlayer) {
            return;
        }

        TrackedMobState state = trackedMobStates.computeIfAbsent(
                npc.getIndex(),
                key -> new TrackedMobState(key, mob)
        );
        if (state.getLastObservedAttackTick() == currentTick) {
            return;
        }

        observeStandardAttack(state, mob, currentTick);
    }

    @Subscribe
    private void onProjectileMoved(ProjectileMoved event) {
        if (!isRuntimeEnabled()) {
            return;
        }

        Projectile projectile = event.getProjectile();
        if (projectile == null || !(projectile.getSourceActor() instanceof NPC)) {
            return;
        }

        Player localPlayer = client.getLocalPlayer();
        if (localPlayer == null || projectile.getTargetActor() != localPlayer) {
            return;
        }

        NPC npc = (NPC) projectile.getSourceActor();
        Mob mob = Mob.fromNpc(npc);
        if (mob == null || mob.isManticore()) {
            return;
        }

        TrackedMobState state = trackedMobStates.computeIfAbsent(
                npc.getIndex(),
                key -> new TrackedMobState(key, mob)
        );

        if (state.getLastProjectileId() == projectile.getId()
                && state.getLastProjectileStartCycle() == projectile.getStartCycle()) {
            return;
        }

        state.setLastProjectileId(projectile.getId());
        state.setLastProjectileStartCycle(projectile.getStartCycle());
        observeStandardAttack(state, mob, client.getTickCount());
    }

    @Subscribe
    private void onGameTick(GameTick event) {
        int currentTick = client.getTickCount();
        lastTickTime = System.currentTimeMillis();

        if (!ctx.isPacketsLoaded()) {
            ctx.initializePackets();
        }

        if (!isRuntimeEnabled()) {
            activeTargetPrayer = null;
            prayerQueue.clear();
            return;
        }

        ColosseumState state = tracker.getCurrentState();
        if (!state.isInColosseum()) {
            clearTrackingState();
            return;
        }

        Player localPlayer = client.getLocalPlayer();
        if (localPlayer == null) {
            clearTrackingState();
            return;
        }

        maintainPrePrayState();

        Map<Integer, NPC> colosseumNpcs = collectColosseumNpcs();
        updateTrackedStates(colosseumNpcs, localPlayer, currentTick);
        resolveManticoreVolleyStarts(currentTick);
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

            boolean hasLineOfSight = hasLineOfSightToPlayer(npc, mob, localPlayer, colosseumNpcs);
            state.setInLineOfSight(hasLineOfSight);

            if (hasLineOfSight && !state.isPreviousLineOfSight()) {
                onLineOfSightGained(state, currentTick);
            } else if (!hasLineOfSight && state.isPreviousLineOfSight()) {
                onLineOfSightLost(state, currentTick);
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
        int currentSpotAnim = currentManticoreSpotAnimationId(npc);
        int previousSpotAnim = state.getLastManticoreSpotAnim();
        state.setLastManticoreSpotAnim(currentSpotAnim);

        if (!hasLineOfSight) {
            state.setSawManticoreIdleInLineOfSight(false);
            return;
        }

        if (currentSpotAnim == -1) {
            state.setSawManticoreIdleInLineOfSight(true);
            return;
        }

        ManticoreAttackStyle spottedStyle = manticoreStyleForSpotAnimation(currentSpotAnim);
        boolean firstStyleSpotAnim = currentSpotAnim == MANTICORE_MAGE_GRAPHIC || currentSpotAnim == MANTICORE_RANGE_GRAPHIC;
        boolean openingStyleTransitioned = state.isSawManticoreIdleInLineOfSight()
                && previousSpotAnim == -1
                && firstStyleSpotAnim;
        boolean manualRepeekStyleSpotted = state.isManualManticoreVolleyPending()
                && previousSpotAnim != currentSpotAnim
                && firstStyleSpotAnim;

        if ((!openingStyleTransitioned && !manualRepeekStyleSpotted) || spottedStyle == null) {
            return;
        }

        int firstAttackTick = currentTick + MANTICORE_SPOT_TO_HIT_TICKS;
        boolean firstVolleyAuto = !state.isManualManticoreVolleyPending();
        state.setManticorePrayerResponsibility(true);
        state.setSynced(true);
        state.setFirstManticoreStyle(spottedStyle);
        state.setFirstVolleyAuto(firstVolleyAuto);
        state.setFirstVolleyTick(firstAttackTick);
        state.setActiveVolleyTick(-1);
        state.setNextVolleyTick(firstAttackTick);
        state.setManualManticoreVolleyPending(false);
        state.setSawManticoreIdleInLineOfSight(false);
    }

    private void resolveManticoreVolleyStarts(int currentTick) {
        List<TrackedMobState> readyManticores = new ArrayList<>();

        for (TrackedMobState state : trackedMobStates.values()) {
            if (!state.getMob().isManticore()
                    || !state.isManticorePrayerResponsibility()
                    || !state.isSynced()) {
                continue;
            }

            int activeVolleyTick = state.getActiveVolleyTick();
            if (activeVolleyTick >= 0 && currentTick > activeVolleyTick + MANTICORE_VOLLEY_SIZE - 1) {
                state.setActiveVolleyTick(-1);
                if (state.getFirstVolleyTick() >= 0 && currentTick > state.getFirstVolleyTick() + MANTICORE_VOLLEY_SIZE - 1) {
                    state.setFirstVolleyTick(-1);
                }
            }

            if (state.getActiveVolleyTick() >= 0) {
                continue;
            }

            if (!state.isInLineOfSight()) {
                continue;
            }

            if (state.getNextVolleyTick() >= 0 && state.getNextVolleyTick() <= currentTick) {
                readyManticores.add(state);
            }
        }

        if (readyManticores.isEmpty()) {
            return;
        }

        readyManticores.sort(Comparator.comparingInt(TrackedMobState::getNpcIndex));

        TrackedMobState attacker = readyManticores.get(0);
        int attackSpeed = Math.max(1, attacker.getMob().getAttackSpeed());
        attacker.setActiveVolleyTick(currentTick);
        attacker.setNextVolleyTick(currentTick + attackSpeed);

        for (int i = 1; i < readyManticores.size(); i++) {
            TrackedMobState delayed = readyManticores.get(i);
            int delayedTick = currentTick + MANTICORE_READY_DELAY_TICKS;
            delayed.setNextVolleyTick(delayedTick);
            if (delayed.getFirstVolleyTick() >= 0 && delayed.getFirstVolleyTick() >= currentTick) {
                delayed.setFirstVolleyTick(delayedTick);
            }
        }
    }

    private ManticoreAttackStyle manticoreStyleForSpotAnimation(int spotAnimId) {
        if (spotAnimId == MANTICORE_MAGE_GRAPHIC) {
            return ManticoreAttackStyle.MAGE;
        }
        if (spotAnimId == MANTICORE_RANGE_GRAPHIC) {
            return ManticoreAttackStyle.RANGE;
        }
        if (spotAnimId == MANTICORE_MELEE_GRAPHIC) {
            return ManticoreAttackStyle.MELEE;
        }
        return null;
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
        if (!state.isSynced() || state.isTentativeAttackSchedule() || mob.getPrayer() == null) {
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
        if (!state.isManticorePrayerResponsibility() || state.getFirstManticoreStyle() == null) {
            return;
        }

        if (!state.isInLineOfSight()) {
            return;
        }

        List<ManticoreAttackStyle> sequence = ManticoreAttackStyle.sequenceForFirst(state.getFirstManticoreStyle());
        if (sequence.isEmpty()) {
            return;
        }

        int activeVolleyTick = state.getActiveVolleyTick();
        if (activeVolleyTick >= 0 && activeVolleyTick + MANTICORE_VOLLEY_SIZE - 1 >= currentTick) {
            queueManticoreVolley(state, sequence, activeVolleyTick, lookahead, currentTick);
        }

        if (!state.isSynced() || state.getNextVolleyTick() < 0) {
            return;
        }

        int attackSpeed = Math.max(1, state.getMob().getAttackSpeed());
        int volleyTick = state.getNextVolleyTick();
        while (volleyTick < currentTick) {
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
        if (!state.isFirstVolleyAuto() && state.getFirstVolleyTick() == volleyStartTick) {
            return;
        }

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

            int predictedAttackTick = predictJaguarAttackTick(npc, localPlayer, state, currentTick, currentNpcs);
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

    private int predictJaguarAttackTick(
            NPC npc,
            Player localPlayer,
            TrackedMobState state,
            int currentTick,
            Map<Integer, NPC> currentNpcs
    ) {
        boolean meleeReachableNow = hasLineOfSightToPlayer(npc, state.getMob(), localPlayer, currentNpcs);
        if (state.isSynced() && state.getNextAttackTick() >= 0 && meleeReachableNow) {
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
            return meleeReachableNow ? currentTick : -1;
        }

        int stepsUntilAdjacent = Math.max(0, pathToPlayer.size() - 1);
        for (int step = 0; step <= stepsUntilAdjacent && step < pathToPlayer.size(); step++) {
            if (collidesWithBlockingNpc(npc, pathToPlayer.get(step), currentNpcs)) {
                return -1;
            }
        }

        return currentTick + stepsUntilAdjacent + 1;
    }

    private boolean collidesWithBlockingNpc(NPC jaguar, WorldPoint jaguarAnchor, Map<Integer, NPC> currentNpcs) {
        if (jaguarAnchor == null) {
            return true;
        }

        int jaguarSize = npcSize(jaguar);
        for (NPC other : currentNpcs.values()) {
            if (other == null || other.getIndex() == jaguar.getIndex() || other.isDead()) {
                continue;
            }

            WorldPoint otherLocation = other.getWorldLocation();
            if (otherLocation == null || otherLocation.getPlane() != jaguarAnchor.getPlane()) {
                continue;
            }

            if (collides(jaguarAnchor, jaguarSize, otherLocation, npcSize(other))) {
                return true;
            }
        }
        return false;
    }

    private int npcSize(NPC npc) {
        if (npc == null || npc.getComposition() == null || npc.getComposition().getSize() <= 0) {
            return 1;
        }
        return npc.getComposition().getSize();
    }

    private boolean collides(WorldPoint first, int firstSize, WorldPoint second, int secondSize) {
        int firstMinX = first.getX();
        int firstMaxX = first.getX() + firstSize - 1;
        int firstMinY = first.getY();
        int firstMaxY = first.getY() + firstSize - 1;

        int secondMinX = second.getX();
        int secondMaxX = second.getX() + secondSize - 1;
        int secondMinY = second.getY();
        int secondMaxY = second.getY() + secondSize - 1;

        return !(firstMaxX < secondMinX
                || secondMaxX < firstMinX
                || firstMaxY < secondMinY
                || secondMaxY < firstMinY);
    }

    private Prayer choosePrayerForCurrentTick(int currentTick) {
        // onGameTick runs after this tick's packets, so only target the next server tick.
        Prayer nextTickPrayer = bestPrayerForTick(currentTick + 1);
        if (nextTickPrayer != null) {
            return nextTickPrayer;
        }

        if (prePrayPrayer != null && prePrayPending) {
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
            if (prePrayPrayer != null && prayerToActivate != prePrayPrayer) {
                clearPrePrayState();
            }

            boolean toggled = prayerService.toggle(prayerToActivate, true);
            if (toggled || client.isPrayerActive(prayerToActivate)) {
                lastAutoActivatedPrayer = prayerToActivate;
                lastAutoActivatedTick = currentTick;
                if (prePrayPrayer != null && prayerToActivate == prePrayPrayer) {
                    prePrayPending = false;
                }
            }
            return;
        }

        if (!oneTickFlickEnabled) {
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
        int nextTick = currentTick + 1;
        Set<Integer> threateningNpcs = new HashSet<>();

        for (PrayerQueueEntry queueEntry : prayerQueue) {
            if (queueEntry.isJaguarPriority() && queueEntry.getTick() == nextTick) {
                return false;
            }

            if (queueEntry.getTick() != nextTick) {
                continue;
            }

            threateningNpcs.add(queueEntry.getNpcIndex());
            if (threateningNpcs.size() > 1) {
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

    private void beginPrePray(int waveNumber) {
        if (!isRuntimeEnabled()) {
            return;
        }

        Prayer prayer = resolveWavePrePrayer(waveNumber);
        if (prayer == null) {
            return;
        }

        prePrayPrayer = prayer;
        prePrayPending = true;
    }

    private void maintainPrePrayState() {
        if (!tracker.getCurrentState().isInColosseum()) {
            clearPrePrayState();
            return;
        }

        if (prePrayPrayer == null) {
            return;
        }

        Prayer activeProtection = getActiveProtectionPrayer();
        if (activeProtection == null) {
            if (!prePrayPending) {
                clearPrePrayState();
            }
            return;
        }

        if (activeProtection != prePrayPrayer) {
            clearPrePrayState();
            return;
        }

        if (prePrayPending) {
            prePrayPending = false;
        }
    }

    private boolean hasLineOfSightToPlayer(NPC npc, Mob mob, Player player, Map<Integer, NPC> currentNpcs) {
        if (npc == null || mob == null || player == null) {
            return false;
        }

        WorldPoint playerPoint = player.getWorldLocation();
        if (playerPoint == null || npc.getWorldLocation() == null) {
            return false;
        }

        List<WorldPoint> visibleTiles = ActorService.getLineOfSightTiles(npc, Math.max(1, mob.getAttackRange()));
        if (!visibleTiles.contains(playerPoint)) {
            return false;
        }

        return !isCornerTrappedByBlockingNpc(npc, player, currentNpcs);
    }

    private boolean isCornerTrappedByBlockingNpc(NPC npc, Player player, Map<Integer, NPC> currentNpcs) {
        if (npc == null || player == null || currentNpcs == null || currentNpcs.isEmpty() || npcSize(npc) != 1) {
            return false;
        }

        WorldPoint npcLocation = npc.getWorldLocation();
        WorldPoint playerLocation = player.getWorldLocation();
        if (npcLocation == null || playerLocation == null || npcLocation.getPlane() != playerLocation.getPlane()) {
            return false;
        }

        int deltaX = playerLocation.getX() - npcLocation.getX();
        int deltaY = playerLocation.getY() - npcLocation.getY();
        if (Math.max(Math.abs(deltaX), Math.abs(deltaY)) > 2) {
            return false;
        }

        int dx = Integer.signum(deltaX);
        int dy = Integer.signum(deltaY);
        if (dx == 0 || dy == 0) {
            return false;
        }

        WorldPoint diagonalStep = new WorldPoint(npcLocation.getX() + dx, npcLocation.getY() + dy, npcLocation.getPlane());
        if (!isCollisionStepOpen(npc, diagonalStep) || !collidesWithBlockingNpc(npc, diagonalStep, currentNpcs)) {
            return false;
        }

        WorldPoint horizontalStep = new WorldPoint(npcLocation.getX() + dx, npcLocation.getY(), npcLocation.getPlane());
        WorldPoint verticalStep = new WorldPoint(npcLocation.getX(), npcLocation.getY() + dy, npcLocation.getPlane());

        return !canTakeImmediateStep(npc, horizontalStep, playerLocation, currentNpcs)
                && !canTakeImmediateStep(npc, verticalStep, playerLocation, currentNpcs);
    }

    private boolean canTakeImmediateStep(NPC npc, WorldPoint step, WorldPoint playerLocation, Map<Integer, NPC> currentNpcs) {
        if (step == null || (playerLocation != null && playerLocation.equals(step))) {
            return false;
        }

        return isCollisionStepOpen(npc, step) && !collidesWithBlockingNpc(npc, step, currentNpcs);
    }

    private boolean isCollisionStepOpen(NPC npc, WorldPoint step) {
        List<WorldPoint> path = ActorService.getActorPath(npc, step);
        return path != null && !path.isEmpty() && step.equals(path.get(0));
    }

    private void onLineOfSightGained(TrackedMobState state, int currentTick) {
        if (state.getMob().isManticore()) {
            return;
        }

        // Player manually handles the first hit after gaining LoS.
        // Treat the follow-up as tentative until an attack animation or projectile confirms the cycle.
        state.setTentativeAttackSchedule(true);
        state.setSynced(false);
        state.setNextAttackTick(currentTick + Math.max(1, state.getMob().getAttackSpeed()));
    }

    private void observeStandardAttack(TrackedMobState state, Mob mob, int currentTick) {
        state.setTentativeAttackSchedule(false);
        state.setSynced(true);
        state.setNextAttackTick(currentTick + Math.max(1, mob.getAttackSpeed()));
        state.setLastObservedAttackTick(currentTick);
    }

    private void onLineOfSightLost(TrackedMobState state, int currentTick) {
        if (state.getMob().isManticore()) {
            // If a volley was ready or already committed when LoS broke, the player owns the re-peek volley.
            boolean attackCommitted = state.getFirstVolleyTick() >= currentTick
                    || (state.getActiveVolleyTick() >= 0
                    && state.getActiveVolleyTick() + MANTICORE_VOLLEY_SIZE - 1 >= currentTick)
                    || (state.getNextVolleyTick() >= 0 && state.getNextVolleyTick() <= currentTick);
            if (attackCommitted && (state.getFirstManticoreStyle() != null || state.isManticorePrayerResponsibility())) {
                state.setManualManticoreVolleyPending(true);
            }
            clearQueuedPrediction(state);
            return;
        }

        if (!config.cancelQueuedOnLosBreak()) {
            return;
        }

        clearQueuedPrediction(state);
    }

    private void clearQueuedPrediction(TrackedMobState state) {
        state.setSynced(false);
        state.setTentativeAttackSchedule(false);
        state.setNextAttackTick(-1);
        state.setLastQueuedTick(-1);

        if (!state.getMob().isManticore()) {
            return;
        }

        boolean preserveManticorePattern = state.isManualManticoreVolleyPending()
                && state.getFirstManticoreStyle() != null;

        state.setFirstVolleyTick(-1);
        state.setFirstVolleyAuto(false);
        state.setActiveVolleyTick(-1);
        state.setNextVolleyTick(-1);
        state.setLastManticoreSpotAnim(-1);
        state.setSawManticoreIdleInLineOfSight(false);
        state.setManticorePrayerResponsibility(false);
        if (!preserveManticorePattern) {
            state.setFirstManticoreStyle(null);
            state.setManualManticoreVolleyPending(false);
        }
    }

    private boolean stateHasFutureAttacks(TrackedMobState state, int currentTick) {
        if (state.getLastQueuedTick() >= currentTick) {
            return true;
        }
        if (!state.getMob().isManticore()) {
            return state.getNextAttackTick() >= currentTick;
        }
        return (state.isFirstVolleyAuto() && state.getFirstVolleyTick() >= currentTick)
                || (state.getActiveVolleyTick() >= 0
                && state.getActiveVolleyTick() + MANTICORE_VOLLEY_SIZE - 1 >= currentTick)
                || state.getNextVolleyTick() >= currentTick;
    }

    private int currentManticoreSpotAnimationId(NPC npc) {
        if (npc == null) {
            return -1;
        }
        if (npc.hasSpotAnim(MANTICORE_MAGE_GRAPHIC)) {
            return MANTICORE_MAGE_GRAPHIC;
        }
        if (npc.hasSpotAnim(MANTICORE_RANGE_GRAPHIC)) {
            return MANTICORE_RANGE_GRAPHIC;
        }
        if (npc.hasSpotAnim(MANTICORE_MELEE_GRAPHIC)) {
            return MANTICORE_MELEE_GRAPHIC;
        }
        return -1;
    }

    private Prayer getActiveProtectionPrayer() {
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
        prePrayPending = false;
    }

    private void toggleRuntimeState() {
        if (!config.enabled()) {
            return;
        }

        runtimeEnabled = !runtimeEnabled;
        clearTrackingState();
        log.info("Auto Colosseum Prayers runtime {}", runtimeEnabled ? "enabled" : "disabled");
    }
}
