package plugins.colosseumv2.engine;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Prayer;
import plugins.colosseumv2.ColosseumConstants;
import plugins.colosseumv2.model.spawns.Mob;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Client-free auto-prayer core for the Fortis Colosseum. Consumes one {@link TickInput} per
 * game tick and produces the {@link PrayerDecision} for the NEXT tick.
 *
 * <p>Timing contract: an attack whose damage is rolled on tick {@code T} must have its
 * protection prayer active before tick {@code T} begins. The engine therefore returns the
 * required prayer while processing tick {@code T - 1} (one tick remaining), and the caller
 * activates it immediately within that same tick.
 *
 * <p>The queue is a projection: it is rebuilt from per-NPC state every tick, which makes
 * cancellation rules (line of sight breaks, deaths, manticore ownership resets) declarative
 * instead of relying on mutation of previously queued entries.
 */
@Slf4j
public class PrayerEngine {

    private final Map<Integer, TrackedNpc> tracked = new TreeMap<>();
    private final List<QueuedAttack> ghosts = new ArrayList<>();

    /** Tick of the last observed attack by a prayable mob, used to close the pre-pray window. */
    private int lastPrayableAttackTick = -1;

    @Getter
    private List<QueuedAttack> queueView = List.of();

    @Getter
    private PrayerDecision lastDecision = PrayerDecision.NONE;

    @Getter
    private Map<Integer, NpcDebugInfo> debugInfo = Map.of();

    @Getter
    private int currentTick = -1;

    /**
     * Clears all engine state. Call when leaving the colosseum or between waves.
     */
    public void reset() {
        tracked.clear();
        ghosts.clear();
        lastPrayableAttackTick = -1;
        queueView = List.of();
        debugInfo = Map.of();
        lastDecision = PrayerDecision.NONE;
    }

    public int getTrackedCount() {
        return tracked.size();
    }

    public TrackedNpc getTracked(int npcIndex) {
        return tracked.get(npcIndex);
    }

    /**
     * Human readable manticore state lines for the debug overlay.
     */
    public List<String> describeManticores() {
        List<String> lines = new ArrayList<>();
        for (TrackedNpc npc : tracked.values()) {
            if (npc.isManticore() && !npc.isDead()) {
                lines.add("Manticore #" + npc.getIndex() + ": " + manticorePhase(npc, currentTick)
                        + (npc.isLosNow() ? " [LoS]" : " [no LoS]"));
            }
        }
        return lines;
    }

    /**
     * Processes one game tick.
     *
     * @param in Snapshot of everything relevant this tick.
     * @return The prayer that must be active on tick {@code in.tick + 1}.
     */
    public PrayerDecision tick(TickInput in) {
        currentTick = in.getTick();
        int tick = currentTick;
        EngineConfig cfg = in.getConfig() != null ? in.getConfig() : EngineConfig.builder().build();
        CollisionMap map = in.getCollisionMap();

        Map<Integer, NpcSnapshot> byIndex = new TreeMap<>();
        for (NpcSnapshot snapshot : in.getNpcs()) {
            byIndex.put(snapshot.getIndex(), snapshot);
        }

        syncTracked(byIndex, in, cfg);

        for (TrackedNpc npc : tracked.values()) {
            updateLineOfSight(npc, map, in);
        }

        for (TrackedNpc npc : tracked.values()) {
            NpcSnapshot snapshot = byIndex.get(npc.getIndex());
            observeAttack(npc, snapshot, tick);
        }

        for (TrackedNpc npc : tracked.values()) {
            if (npc.isManticore()) {
                updateManticoreCharge(npc, byIndex.get(npc.getIndex()), tick);
            }
        }

        for (TrackedNpc npc : tracked.values()) {
            maintainSchedules(npc, tick);
        }

        for (TrackedNpc npc : tracked.values()) {
            npc.setObservedPrevTick(true);
            NpcSnapshot snapshot = byIndex.get(npc.getIndex());
            if (snapshot != null) {
                npc.setHadOrbPrev(snapshot.hasAnyOrb());
            }
        }

        Map<Integer, ColosseumPathing.Forecast> forecasts = runForwardSimulation(in, cfg, true);

        List<QueuedAttack> entries = buildProjection(tick, cfg, forecasts);
        entries.sort(Comparator
                .comparingInt(QueuedAttack::getLandTick)
                .thenComparing(Comparator.comparingInt(QueuedAttack::getConflictMaxHit).reversed())
                .thenComparingInt(QueuedAttack::getNpcIndex));
        queueView = List.copyOf(entries);

        PrayerDecision decision = decide(entries, in, cfg, tick);
        lastDecision = decision;

        buildDebugInfo(in, cfg, forecasts);

        return decision;
    }

    // =====================
    // Tracking
    // =====================

    private void syncTracked(Map<Integer, NpcSnapshot> byIndex, TickInput in, EngineConfig cfg) {
        int tick = in.getTick();

        Iterator<Map.Entry<Integer, TrackedNpc>> iterator = tracked.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, TrackedNpc> entry = iterator.next();
            NpcSnapshot snapshot = byIndex.get(entry.getKey());
            boolean replaced = snapshot != null && snapshot.getNpcId() != entry.getValue().getNpcId();
            if (snapshot == null || replaced) {
                if (!cfg.isRemoveQueuedOnNpcDeath()) {
                    List<QueuedAttack> remnants = new ArrayList<>();
                    emitFor(entry.getValue(), tick, cfg, remnants);
                    for (QueuedAttack remnant : remnants) {
                        ghosts.add(remnant.toBuilder().source(QueuedAttack.Source.GHOST).build());
                    }
                }
                iterator.remove();
            }
        }

        for (NpcSnapshot snapshot : byIndex.values()) {
            TrackedNpc npc = tracked.get(snapshot.getIndex());
            if (npc == null) {
                npc = new TrackedNpc(snapshot.getIndex(), snapshot.getNpcId(), snapshot.getName(),
                        Mob.fromIdAndName(snapshot.getNpcId(), snapshot.getName()));
                npc.setFirstSeenTick(tick);
                boolean waveSpawn = in.isWaveStarted()
                        && in.getWaveStartTick() >= 0
                        && tick - in.getWaveStartTick() <= 2;
                npc.setReadyTick(waveSpawn
                        ? in.getWaveStartTick() + ColosseumConstants.DELAY_FIRST_ATTACK_TICKS
                        : tick);
                tracked.put(snapshot.getIndex(), npc);
            }
            npc.updateFromSnapshot(snapshot);
        }

        ghosts.removeIf(ghost -> ghost.getLandTick() <= tick);
    }

    private void updateLineOfSight(TrackedNpc npc, CollisionMap map, TickInput in) {
        npc.setLosPrev(npc.isLosNow());

        Mob mob = npc.getMob();
        if (mob == null || map == null || npc.isDead()) {
            npc.setLosNow(false);
            return;
        }

        boolean los = ColosseumPathing.canAttack(
                map,
                npc.getSceneX(), npc.getSceneY(), npc.getSize(),
                mob.getAttackRange(), mob.isMeleeOnly(),
                in.getPlayerX(), in.getPlayerY());
        npc.setLosNow(los);
    }

    // =====================
    // Attack observation
    // =====================

    private void observeAttack(TrackedNpc npc, NpcSnapshot snapshot, int tick) {
        if (snapshot == null || npc.getMob() == null) {
            return;
        }

        Mob mob = npc.getMob();
        if (!snapshot.isAnimationChanged() || !mob.isAttackAnimation(snapshot.getAnimation())) {
            return;
        }

        npc.setLastAttackTick(tick);
        npc.setAttackCount(npc.getAttackCount() + 1);
        npc.setReadyTick(tick + mob.getAttackSpeed());

        if (npc.isPrayable()) {
            lastPrayableAttackTick = tick;
        }

        if (npc.isManticore()) {
            onVolleyStart(npc, tick);
        } else {
            npc.setNextAttackTick(tick + mob.getAttackSpeed());
        }
    }

    private void onVolleyStart(TrackedNpc manticore, int tick) {
        boolean predicted = manticore.getVolleyReadyTick() == tick;
        manticore.setCurrentVolleyPluginOwned(manticore.isWitnessOwned() || predicted);
        manticore.setWitnessOwned(false);
        manticore.setLastVolleyStartTick(tick);
        manticore.setVolleyReadyTick(tick + ColosseumConstants.MANTICORE_CHARGE_TICKS);
        manticore.setReadyTick(tick + ColosseumConstants.MANTICORE_CHARGE_TICKS);

        // A firing manticore staggers every other ready manticore by 5 ticks.
        for (TrackedNpc other : tracked.values()) {
            if (other == manticore || !other.isManticore() || other.isDead()) {
                continue;
            }
            if (other.getVolleyReadyTick() != -1 && other.getVolleyReadyTick() <= tick) {
                other.setVolleyReadyTick(tick + ColosseumConstants.MANTICORE_STAGGER_DELAY);
            }
        }
    }

    // =====================
    // Manticore charge machine
    // =====================

    private void updateManticoreCharge(TrackedNpc npc, NpcSnapshot snapshot, int tick) {
        if (snapshot == null) {
            return;
        }

        boolean hasAnyOrb = snapshot.hasAnyOrb();

        if (!npc.isEverCharged() && hasAnyOrb) {
            npc.setEverCharged(true);
            npc.setChargeStartTick(tick);

            boolean mageOnly = snapshot.isHasMageOrb() && !snapshot.isHasRangeOrb();
            boolean rangeOnly = snapshot.isHasRangeOrb() && !snapshot.isHasMageOrb();
            if (mageOnly) {
                npc.setMageFirst(true);
            } else if (rangeOnly) {
                npc.setMageFirst(false);
            } else {
                npc.setMageFirst(null);
            }

            // The plugin only owns the first volley when it witnessed the clean transition
            // from no orbs to the first mage/range orb while the player was in line of sight.
            boolean cleanTransition = npc.isObservedPrevTick()
                    && !npc.isHadOrbPrev()
                    && !snapshot.isHasMeleeOrb()
                    && npc.getMageFirst() != null;

            if (cleanTransition && npc.isLosNow() && !npc.isDead()) {
                npc.setWitnessOwned(true);
                npc.setVolleyReadyTick(tick + ColosseumConstants.MANTICORE_CHARGE_TICKS);
                log.debug("Witnessed manticore {} charge start on tick {}, first style {}",
                        npc.getIndex(), tick, npc.getMageFirst() ? "mage" : "range");
            }
        }

        // Any break in line of sight before the first volley fires puts the whole volley back
        // on the player. The plugin never handles partial volleys.
        if (npc.isWitnessOwned() && !npc.isLosNow()) {
            npc.setWitnessOwned(false);
            npc.setVolleyReadyTick(-1);
            log.debug("Manticore {} line of sight broken during charge, ownership reset", npc.getIndex());
        }
    }

    // =====================
    // Schedule maintenance
    // =====================

    private void maintainSchedules(TrackedNpc npc, int tick) {
        Mob mob = npc.getMob();
        if (mob == null) {
            return;
        }

        boolean attackedThisTick = npc.getLastAttackTick() == tick;

        if (!npc.isManticore()) {
            if (npc.getNextAttackTick() == tick && !attackedThisTick) {
                // The predicted attack tick arrived without an observed animation. If the mob
                // still has line of sight it attacked (or will resync next observation); keep
                // the chain rolling. Otherwise the chain is broken and the player owns the
                // next first attack.
                if (npc.isLosNow() && !npc.isDead()) {
                    npc.setNextAttackTick(tick + mob.getAttackSpeed());
                    npc.setReadyTick(tick + mob.getAttackSpeed());
                } else {
                    npc.setNextAttackTick(-1);
                }
            }
            return;
        }

        if (npc.getVolleyReadyTick() == tick && !attackedThisTick) {
            if (npc.isLosNow() && !npc.isDead()) {
                // Predicted volley tick reached without the throw animation being seen yet;
                // assume it fired to keep orb timing aligned.
                onVolleyStart(npc, tick);
            } else {
                // Manticore is holding its volley without line of sight; the release tick is
                // now unknowable, so the player owns the next volley.
                npc.setWitnessOwned(false);
                npc.setVolleyReadyTick(-1);
            }
        }
    }

    // =====================
    // Forward simulation (pathing predictions)
    // =====================
    private Map<Integer, ColosseumPathing.Forecast> runForwardSimulation(TickInput in, EngineConfig cfg, boolean stopOnLos) {
        boolean needed = cfg.isPrayJaguarOnPath() || cfg.isDebugPaths();
        if (!needed || in.getCollisionMap() == null) {
            return Map.of();
        }

        List<ColosseumPathing.Mover> movers = new ArrayList<>();
        List<int[]> staticOccupancy = new ArrayList<>();

        for (TrackedNpc npc : tracked.values()) {
            if (npc.isDead()) {
                continue;
            }
            if (npc.getNpcId() == ColosseumConstants.HEALING_TOTEM) {
                staticOccupancy.add(new int[]{npc.getSceneX(), npc.getSceneY(), npc.getSize()});
                continue;
            }

            int range;
            boolean meleeOnly;
            Mob mob = npc.getMob();
            if (mob != null) {
                range = mob.getAttackRange();
                meleeOnly = mob.isMeleeOnly();
            } else if (npc.getNpcId() == ColosseumConstants.WARBAND_BERSERKER) {
                range = 1;
                meleeOnly = true;
            } else {
                // Warband archer/seer approximation; only influences where they stop.
                range = 8;
                meleeOnly = false;
            }

            movers.add(new ColosseumPathing.Mover(
                    npc.getIndex(),
                    npc.getSceneX(), npc.getSceneY(), npc.getSize(),
                    range, meleeOnly,
                    Math.max(npc.getReadyTick(), in.getTick() + 1)));
        }

        if (movers.isEmpty()) {
            return Map.of();
        }

        return ColosseumPathing.forwardSimulate(
                in.getCollisionMap(),
                movers,
                staticOccupancy,
                in.getPlayerX(), in.getPlayerY(),
                in.getTick(),
                ColosseumConstants.PATH_PREDICTION_HORIZON,
                stopOnLos);
    }

    // =====================
    // Queue projection
    // =====================

    private List<QueuedAttack> buildProjection(int tick, EngineConfig cfg, Map<Integer, ColosseumPathing.Forecast> forecasts) {
        Map<Long, QueuedAttack> merged = new LinkedHashMap<>();

        List<QueuedAttack> raw = new ArrayList<>();
        for (TrackedNpc npc : tracked.values()) {
            emitFor(npc, tick, cfg, raw);

            if (cfg.isPrayJaguarOnPath()
                    && npc.getMob() != null
                    && npc.getMob().isJaguarWarrior()
                    && !npc.isDead()) {
                ColosseumPathing.Forecast forecast = forecasts.get(npc.getIndex());
                if (forecast != null && forecast.firstAttackTick > tick
                        && forecast.firstAttackTick <= tick + cfg.getQueueLookaheadTicks()) {
                    raw.add(QueuedAttack.builder()
                            .npcIndex(npc.getIndex())
                            .npcName(npc.getName())
                            .mob(npc.getMob())
                            .prayer(Prayer.PROTECT_FROM_MELEE)
                            .landTick(forecast.firstAttackTick)
                            .source(QueuedAttack.Source.JAGUAR_PATH)
                            .conflictMaxHit(npc.getMob().getConflictMaxHit())
                            .jaguarPriority(true)
                            .styleLabel("Melee")
                            .build());
                }
            }
        }

        raw.addAll(ghosts);

        for (QueuedAttack attack : raw) {
            if (attack.getLandTick() <= tick || attack.getLandTick() > tick + cfg.getQueueLookaheadTicks()) {
                continue;
            }
            long key = ((long) attack.getLandTick() << 32) | (attack.getNpcIndex() & 0xffffffffL);
            QueuedAttack existing = merged.get(key);
            if (existing == null) {
                merged.put(key, attack);
            } else {
                merged.put(key, existing.toBuilder()
                        .jaguarPriority(existing.isJaguarPriority() || attack.isJaguarPriority())
                        .artilleryLikely(existing.isArtilleryLikely() && attack.isArtilleryLikely())
                        .conflictMaxHit(Math.max(existing.getConflictMaxHit(), attack.getConflictMaxHit()))
                        .build());
            }
        }

        return new ArrayList<>(merged.values());
    }

    /**
     * Emits queue entries derived from one NPC's current state. Also used to freeze entries
     * into ghosts when an NPC despawns and removal-on-death is disabled.
     */
    private void emitFor(TrackedNpc npc, int tick, EngineConfig cfg, List<QueuedAttack> out) {
        Mob mob = npc.getMob();
        if (mob == null || !npc.isPrayable()) {
            return;
        }
        if (npc.isDead() && cfg.isRemoveQueuedOnNpcDeath()) {
            return;
        }

        if (!npc.isManticore()) {
            // A dead NPC with removal disabled keeps its frozen entries; a live NPC's entries
            // are gated on line of sight per configuration.
            boolean losOk = npc.isDead() || npc.isLosNow() || !cfg.isCancelQueuedOnLosBreak();
            if (npc.getNextAttackTick() > tick && losOk) {
                boolean artilleryLikely = mob == Mob.JAVELIN_COLOSSUS && npc.getAttackCount() % 5 == 4;
                out.add(QueuedAttack.builder()
                        .npcIndex(npc.getIndex())
                        .npcName(npc.getName())
                        .mob(mob)
                        .prayer(mob.getPrayer())
                        .landTick(npc.getNextAttackTick())
                        .source(QueuedAttack.Source.CHAIN)
                        .conflictMaxHit(mob.getConflictMaxHit())
                        .artilleryLikely(artilleryLikely)
                        .styleLabel(styleLabel(mob.getPrayer()))
                        .build());
            }
            return;
        }

        if (npc.getMageFirst() == null) {
            return;
        }

        // Remainder of an in-progress plugin-owned volley (orbs two and three).
        if (npc.getLastVolleyStartTick() >= 0 && npc.isCurrentVolleyPluginOwned()) {
            for (int orb = 1; orb < ColosseumConstants.MANTICORE_VOLLEY_SIZE; orb++) {
                int landTick = npc.getLastVolleyStartTick() + orb;
                if (landTick > tick) {
                    out.add(manticoreOrb(npc, orb, landTick, QueuedAttack.Source.MANTICORE_CHAIN));
                }
            }
        }

        // Upcoming volley: witnessed first volley, or chain-predicted while line of sight holds.
        boolean upcoming = npc.getVolleyReadyTick() > tick
                && (npc.isWitnessOwned() || (npc.isLosNow() && npc.getLastVolleyStartTick() >= 0));
        if (upcoming) {
            QueuedAttack.Source source = npc.isWitnessOwned()
                    ? QueuedAttack.Source.MANTICORE_WITNESSED
                    : QueuedAttack.Source.MANTICORE_CHAIN;
            for (int orb = 0; orb < ColosseumConstants.MANTICORE_VOLLEY_SIZE; orb++) {
                int landTick = npc.getVolleyReadyTick() + orb;
                if (landTick > tick) {
                    out.add(manticoreOrb(npc, orb, landTick, source));
                }
            }
        }
    }

    private QueuedAttack manticoreOrb(TrackedNpc npc, int orbIndex, int landTick, QueuedAttack.Source source) {
        Prayer prayer;
        String label;
        if (orbIndex == 2) {
            prayer = Prayer.PROTECT_FROM_MELEE;
            label = "Melee";
        } else {
            boolean mage = (orbIndex == 0) == Boolean.TRUE.equals(npc.getMageFirst());
            prayer = mage ? Prayer.PROTECT_FROM_MAGIC : Prayer.PROTECT_FROM_MISSILES;
            label = mage ? "Mage" : "Range";
        }

        return QueuedAttack.builder()
                .npcIndex(npc.getIndex())
                .npcName(npc.getName())
                .mob(npc.getMob())
                .prayer(prayer)
                .landTick(landTick)
                .source(source)
                .conflictMaxHit(ColosseumConstants.MANTICORE_ORB_MAX_HIT)
                .styleLabel(label)
                .build();
    }

    private String styleLabel(Prayer prayer) {
        if (prayer == Prayer.PROTECT_FROM_MAGIC) {
            return "Mage";
        }
        if (prayer == Prayer.PROTECT_FROM_MISSILES) {
            return "Range";
        }
        if (prayer == Prayer.PROTECT_FROM_MELEE) {
            return "Melee";
        }
        return "?";
    }

    // =====================
    // Decision
    // =====================

    private PrayerDecision decide(List<QueuedAttack> entries, TickInput in, EngineConfig cfg, int tick) {
        List<QueuedAttack> nextTick = new ArrayList<>();
        for (QueuedAttack attack : entries) {
            if (attack.getLandTick() == tick + 1 && attack.getPrayer() != null) {
                nextTick.add(attack);
            }
        }

        if (!nextTick.isEmpty()) {
            QueuedAttack winner = nextTick.get(0);
            for (QueuedAttack candidate : nextTick) {
                if (beats(candidate, winner)) {
                    winner = candidate;
                }
            }
            return new PrayerDecision(winner.getPrayer(), PrayerDecision.Reason.QUEUED_ATTACK, winner, nextTick.size());
        }

        if (isPrePrayActive(in, cfg, tick)) {
            return new PrayerDecision(in.getPrePrayPrayer(), PrayerDecision.Reason.PRE_PRAY, null, 0);
        }

        return PrayerDecision.NONE;
    }

    private boolean beats(QueuedAttack candidate, QueuedAttack current) {
        if (candidate.isJaguarPriority() != current.isJaguarPriority()) {
            return candidate.isJaguarPriority();
        }
        if (candidate.isArtilleryLikely() != current.isArtilleryLikely()) {
            // Artillery ignores prayer, so a likely-artillery entry loses conflicts.
            return current.isArtilleryLikely();
        }
        if (candidate.getConflictMaxHit() != current.getConflictMaxHit()) {
            return candidate.getConflictMaxHit() > current.getConflictMaxHit();
        }
        return false;
    }

    private boolean isPrePrayActive(TickInput in, EngineConfig cfg, int tick) {
        if (!cfg.isEnableWavePrePray() || in.getPrePrayPrayer() == null || !in.isInColosseum()) {
            return false;
        }

        if (!in.isWaveStarted()) {
            // Intermission: only pre-pray while the arena is actually clear of live threats,
            // which avoids holding a prayer forever when wave state is unknown (mid-wave login).
            for (TrackedNpc npc : tracked.values()) {
                if (npc.isPrayable() && !npc.isDead()) {
                    return false;
                }
            }
            return true;
        }

        if (in.getWaveStartTick() < 0) {
            return false;
        }

        boolean inWindow = tick - in.getWaveStartTick() <= cfg.getPrePrayWindowTicks();
        boolean noAttackYet = lastPrayableAttackTick < in.getWaveStartTick();
        return inWindow && noAttackYet;
    }

    // =====================
    // Debug
    // =====================

    private void buildDebugInfo(TickInput in, EngineConfig cfg, Map<Integer, ColosseumPathing.Forecast> forecasts) {
        if (!cfg.isDebugLosTiles() && !cfg.isDebugPaths()) {
            debugInfo = Map.of();
            return;
        }

        Map<Integer, ColosseumPathing.Forecast> pathForecasts = forecasts;
        if (cfg.isDebugPaths() && !cfg.isDebugStopPathOnLos()) {
            pathForecasts = runForwardSimulation(in, cfg, false);
        }

        List<TrackedNpc> nearest = new ArrayList<>(tracked.values());
        nearest.sort(Comparator.comparingInt(npc ->
                ColosseumPathing.distanceTo(npc.getSceneX(), npc.getSceneY(), npc.getSize(), in.getPlayerX(), in.getPlayerY())));

        Map<Integer, NpcDebugInfo> result = new HashMap<>();
        int rendered = 0;
        int tick = in.getTick();

        for (TrackedNpc npc : nearest) {
            if (rendered++ >= cfg.getDebugMaxNpcs()) {
                break;
            }

            ColosseumPathing.Forecast forecast = pathForecasts.get(npc.getIndex());
            List<int[]> path = List.of();
            if (cfg.isDebugPaths() && forecast != null) {
                int limit = Math.min(forecast.path.size(), cfg.getDebugPathLength());
                path = new ArrayList<>(forecast.path.subList(0, limit));
            }

            List<int[]> losTiles = List.of();
            if (cfg.isDebugLosTiles() && npc.getMob() != null && !npc.isDead() && in.getCollisionMap() != null) {
                losTiles = ColosseumPathing.visibleTiles(
                        in.getCollisionMap(),
                        npc.getSceneX(), npc.getSceneY(), npc.getSize(),
                        npc.getMob().getAttackRange(), npc.getMob().isMeleeOnly(),
                        npc.getMob().getAttackRange());
            }

            String manticorePhase = null;
            if (npc.isManticore()) {
                manticorePhase = manticorePhase(npc, tick);
            }

            result.put(npc.getIndex(), NpcDebugInfo.builder()
                    .npcIndex(npc.getIndex())
                    .name(npc.getName())
                    .sceneX(npc.getSceneX())
                    .sceneY(npc.getSceneY())
                    .size(npc.getSize())
                    .lineOfSight(npc.isLosNow())
                    .stuck(forecast != null && forecast.stuck)
                    .readyIn(npc.getReadyTick() > tick ? npc.getReadyTick() - tick : 0)
                    .lastAttackAgo(npc.getLastAttackTick() >= 0 ? tick - npc.getLastAttackTick() : -1)
                    .nextAttackTick(npc.isManticore() ? npc.getVolleyReadyTick() : npc.getNextAttackTick())
                    .pathAttackTick(forecast != null ? forecast.firstAttackTick : -1)
                    .manticorePhase(manticorePhase)
                    .path(path)
                    .losTiles(losTiles)
                    .build());
        }

        debugInfo = result;
    }

    private String manticorePhase(TrackedNpc npc, int tick) {
        if (!npc.isEverCharged()) {
            return "UNCHARGED";
        }

        String style = npc.getMageFirst() == null ? "?" : (npc.getMageFirst() ? "mage" : "range");
        if (npc.getLastVolleyStartTick() >= 0 && tick - npc.getLastVolleyStartTick() < ColosseumConstants.MANTICORE_VOLLEY_SIZE) {
            return "VOLLEY (" + style + " first)";
        }
        if (npc.getVolleyReadyTick() > tick) {
            String owner = npc.isWitnessOwned() ? "plugin" : "chain";
            return "CHARGED " + style + ", fires in " + (npc.getVolleyReadyTick() - tick) + "t (" + owner + ")";
        }
        if (npc.getVolleyReadyTick() == -1) {
            return "CHARGED " + style + " (player prays first volley)";
        }
        return "CHARGED " + style;
    }
}
