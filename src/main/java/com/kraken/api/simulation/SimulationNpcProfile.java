package com.kraken.api.simulation;

import lombok.Builder;
import lombok.Getter;
import lombok.With;
import lombok.extern.slf4j.Slf4j;

/**
 * NPC combat and movement profile used while simulating each NPC id.
 */
@Slf4j
@Getter
@With
public final class SimulationNpcProfile {
    private static final int DEFAULT_ATTACK_SPEED = 4;

    /**
     * Default profile used when no id mapping exists.
     */
    public static final SimulationNpcProfile DEFAULT = new SimulationNpcProfile(1, NpcAttackStyle.UNKNOWN, DEFAULT_ATTACK_SPEED, 0, false);

    private final int attackRange;
    private final NpcAttackStyle attackStyle;
    private final int attackSpeed;
    private final int maxHit;
    private final boolean intelligentPathing;

    /**
     * Creates a profile for an NPC id mapping.
     *
     * @param attackRange attack range used for line-of-sight and attack checks.
     * @param attackStyle attack style used for protection-prayer checks.
     * @param attackSpeed attack speed in ticks.
     * @param maxHit max hit used for damage simulation.
     * @param intelligentPathing true to use collision-aware pathfinding instead of greedy movement.
     */
    @Builder(toBuilder = true)
    public SimulationNpcProfile(
            int attackRange,
            NpcAttackStyle attackStyle,
            int attackSpeed,
            int maxHit,
            boolean intelligentPathing
    ) {
        if (attackRange <= 0) {
            throw new IllegalArgumentException("attackRange must be > 0");
        }
        if (maxHit < 0) {
            throw new IllegalArgumentException("maxHit must be >= 0");
        }
        if (attackSpeed <= 0) {
            log.warn("Invalid attack speed value passed, Using default attack speed of 4");
            attackSpeed = 4;
        }
        this.attackRange = attackRange;
        this.attackStyle = attackStyle == null ? NpcAttackStyle.UNKNOWN : attackStyle;
        this.attackSpeed = attackSpeed;
        this.maxHit = maxHit;
        this.intelligentPathing = intelligentPathing;
    }
}
