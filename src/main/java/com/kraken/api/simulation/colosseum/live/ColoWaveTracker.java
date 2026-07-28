package com.kraken.api.simulation.colosseum.live;

import com.kraken.api.simulation.colosseum.ColoConstants;
import com.kraken.api.simulation.colosseum.ColoNpcType;
import com.kraken.api.simulation.colosseum.ColoTick;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Actor;
import net.runelite.api.NPC;
import net.runelite.api.Player;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Persistent per-wave observation state that a single snapshot cannot provide: attack
 * cooldowns (from observed attack animations), manticore charge states and patterns (from
 * spot-anims), javelin special counters, the warband attack-cycle phase, and the wave tick.
 *
 * <p>The hosting plugin forwards RuneLite events into this tracker
 * ({@link #onGameTick()}, {@link #onNpcSpawned}, {@link #onNpcDespawned},
 * {@link #onAnimationChanged}); {@link ColoCapture} then reads it while building the
 * simulation state. Everything here is an estimate that self-corrects every tick - the
 * planner replans from a fresh capture each tick, which is what makes the system adapt to
 * surprises (reinforcements, missed observations, lag).</p>
 */
public final class ColoWaveTracker {
    /**
     * Per-NPC observation record.
     */
    public static final class NpcRecord {
        @Getter
        private final int npcIndex;
        @Getter
        private final ColoNpcType type;
        private NPC npc;
        int lastAttackTick = Integer.MIN_VALUE;
        int attackCount;
        int chargeSeenTick = Integer.MIN_VALUE;
        int pattern = ColoTick.PATTERN_UNKNOWN;
        int lastKnownHp;

        NpcRecord(int npcIndex, ColoNpcType type, NPC npc) {
            this.npcIndex = npcIndex;
            this.type = type;
            this.npc = npc;
            this.lastKnownHp = type.getMaxHitpoints();
        }

        /** @return live RuneLite npc reference. */
        public NPC npc() {
            return npc;
        }

        /** @return manticore pattern code observed from spot-anims. */
        public int pattern() {
            return pattern;
        }
    }

    private final Map<Integer, NpcRecord> records = new LinkedHashMap<>();

    /** Ticks since the current wave started, -1 when no wave is active. */
    @Getter
    private int waveTick = -1;

    /** Observed warband attack-cycle phase (tick % 6 of their attacks). */
    @Getter
    private int warbandCyclePhase = ColoConstants.DEFAULT_WARBAND_CYCLE_PHASE;

    /**
     * Spot-anim id shown by a manticore charging a ranged-first triple; -1 disables
     * spot-anim pattern detection. Configure from the hosting plugin.
     */
    @Getter
    @Setter
    private int manticoreRangedSpotAnim = -1;

    /** Spot-anim id for a magic-first manticore charge; -1 disables detection. */
    @Getter
    @Setter
    private int manticoreMagicSpotAnim = -1;

    private int playerLastAttackTick = Integer.MIN_VALUE;
    private int playerLastEatTick = Integer.MIN_VALUE;

    /**
     * Advances the wave clock and refreshes per-NPC spot-anim observations. Call once per
     * game tick before capturing.
     */
    public void onGameTick() {
        if (waveTick >= 0) {
            waveTick++;
        }
        Iterator<NpcRecord> iterator = records.values().iterator();
        while (iterator.hasNext()) {
            NpcRecord record = iterator.next();
            NPC npc = record.npc;
            if (npc == null) {
                continue;
            }
            if (record.type == ColoNpcType.MANTICORE) {
                if (manticoreRangedSpotAnim >= 0 && npc.hasSpotAnim(manticoreRangedSpotAnim)) {
                    record.pattern = ColoTick.PATTERN_RANGED_FIRST;
                    if (record.chargeSeenTick == Integer.MIN_VALUE) {
                        record.chargeSeenTick = waveTick;
                    }
                } else if (manticoreMagicSpotAnim >= 0 && npc.hasSpotAnim(manticoreMagicSpotAnim)) {
                    record.pattern = ColoTick.PATTERN_MAGIC_FIRST;
                    if (record.chargeSeenTick == Integer.MIN_VALUE) {
                        record.chargeSeenTick = waveTick;
                    }
                }
            }
            int healthRatio = npc.getHealthRatio();
            int healthScale = npc.getHealthScale();
            if (healthRatio >= 0 && healthScale > 0) {
                record.lastKnownHp = Math.max(1, record.type.getMaxHitpoints() * healthRatio / healthScale);
            }
        }
    }

    /**
     * Registers a spawned colosseum NPC; the first spawn into an empty arena marks the
     * start of a wave.
     *
     * @param npc spawned npc.
     */
    public void onNpcSpawned(NPC npc) {
        ColoNpcType type = ColoNpcType.fromNpcId(npc.getId());
        if (type == null) {
            return;
        }
        if (records.isEmpty()) {
            waveTick = 0;
        }
        records.put(npc.getIndex(), new NpcRecord(npc.getIndex(), type, npc));
    }

    /**
     * Removes a despawned NPC; an empty arena ends the wave clock.
     *
     * @param npc despawned npc.
     */
    public void onNpcDespawned(NPC npc) {
        records.remove(npc.getIndex());
        if (records.isEmpty()) {
            waveTick = -1;
        }
    }

    /**
     * Records attack animations for cooldown estimation. Colosseum NPCs only play
     * non-pose animations when attacking (or dying), so any animation change while
     * tracked is treated as an attack launch; estimates self-correct next cycle.
     *
     * @param actor actor whose animation changed.
     * @param localPlayer the local player, for player cooldown tracking.
     */
    public void onAnimationChanged(Actor actor, Player localPlayer) {
        if (actor == localPlayer && actor.getAnimation() != -1) {
            playerLastAttackTick = waveTick;
            return;
        }
        if (!(actor instanceof NPC)) {
            return;
        }
        NpcRecord record = records.get(((NPC) actor).getIndex());
        if (record == null || actor.getAnimation() == -1) {
            return;
        }
        record.lastAttackTick = waveTick;
        record.attackCount++;
        if (record.type.isWarband() && waveTick >= 0) {
            warbandCyclePhase = Math.floorMod(waveTick, ColoConstants.WARBAND_CYCLE_TICKS);
        }
    }

    /**
     * Marks that the player just ate (hosting plugins may call this from inventory or
     * chat events to tighten the consumption-timer estimates).
     */
    public void onPlayerAte() {
        playerLastEatTick = waveTick;
    }

    /**
     * Manually restarts the wave clock (e.g. from a wave-start chat message).
     */
    public void markWaveStart() {
        waveTick = 0;
    }

    /**
     * Clears all observations.
     */
    public void reset() {
        records.clear();
        waveTick = -1;
        playerLastAttackTick = Integer.MIN_VALUE;
        playerLastEatTick = Integer.MIN_VALUE;
    }

    /** @return true when a wave is currently active. */
    public boolean isWaveActive() {
        return waveTick >= 0;
    }

    /** @return tracked npc records in spawn order. */
    public Iterable<NpcRecord> records() {
        return records.values();
    }

    /**
     * @param npcIndex RuneLite npc index.
     * @return record for the index, or null when untracked.
     */
    public NpcRecord record(int npcIndex) {
        return records.get(npcIndex);
    }

    /**
     * Estimates an NPC's current attack cooldown from its last observed attack.
     *
     * @param record npc record.
     * @return estimated ticks until the npc can act (0 when unknown - conservative).
     */
    public int cooldownEstimate(NpcRecord record) {
        if (record.type == ColoNpcType.MANTICORE) {
            if (record.lastAttackTick != Integer.MIN_VALUE) {
                int sinceFire = waveTick - record.lastAttackTick;
                return Math.max(0, record.type.getAttackSpeedTicks() - sinceFire);
            }
            if (record.chargeSeenTick != Integer.MIN_VALUE) {
                return Math.max(0, ColoConstants.MANTICORE_CHARGE_TICKS - (waveTick - record.chargeSeenTick));
            }
            return 0;
        }
        if (record.lastAttackTick == Integer.MIN_VALUE) {
            return 0;
        }
        return Math.max(0, record.type.getAttackSpeedTicks() - (waveTick - record.lastAttackTick));
    }

    /**
     * @param record manticore record.
     * @return orbs remaining in an in-progress triple (0 outside one).
     */
    public int manticoreOrbsRemaining(NpcRecord record) {
        if (record.type != ColoNpcType.MANTICORE || record.lastAttackTick == Integer.MIN_VALUE) {
            return 0;
        }
        int sinceFire = waveTick - record.lastAttackTick;
        if (sinceFire < 0 || sinceFire >= ColoConstants.MANTICORE_ORB_COUNT) {
            return 0;
        }
        return ColoConstants.MANTICORE_ORB_COUNT - 1 - sinceFire;
    }

    /**
     * @param record manticore record.
     * @return true when the manticore has begun (or finished) its charge.
     */
    public boolean manticoreChargingStarted(NpcRecord record) {
        return record.chargeSeenTick != Integer.MIN_VALUE || record.lastAttackTick != Integer.MIN_VALUE;
    }

    /**
     * @param record javelin record.
     * @return auto attacks since the last sky javelin, 0-4.
     */
    public int javelinAutosSinceSpecial(NpcRecord record) {
        return record.attackCount % ColoConstants.JAVELIN_SPECIAL_EVERY_N_ATTACKS;
    }

    /**
     * @param record npc record.
     * @return last known hitpoints (from health bars; max HP until first damage).
     */
    public int lastKnownHp(NpcRecord record) {
        return record.lastKnownHp;
    }

    /**
     * @param weaponSpeedTicks current weapon speed.
     * @return estimated ticks until the player may attack again.
     */
    public int playerAttackDelayEstimate(int weaponSpeedTicks) {
        if (playerLastAttackTick == Integer.MIN_VALUE) {
            return 0;
        }
        return Math.max(0, weaponSpeedTicks - (waveTick - playerLastAttackTick));
    }

    /**
     * @return estimated ticks until the player may eat food again.
     */
    public int playerFoodDelayEstimate() {
        if (playerLastEatTick == Integer.MIN_VALUE) {
            return 0;
        }
        return Math.max(0, 3 - (waveTick - playerLastEatTick));
    }
}
