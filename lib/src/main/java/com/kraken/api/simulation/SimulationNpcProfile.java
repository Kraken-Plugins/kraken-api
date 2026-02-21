package com.kraken.api.simulation;

import lombok.Getter;

/**
 * NPC combat and movement profile used while simulating each NPC id.
 */
@Getter
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
        if (attackSpeed <= 0) {
            throw new IllegalArgumentException("attackSpeed must be > 0");
        }
        if (maxHit < 0) {
            throw new IllegalArgumentException("maxHit must be >= 0");
        }
        this.attackRange = attackRange;
        this.attackStyle = attackStyle == null ? NpcAttackStyle.UNKNOWN : attackStyle;
        this.attackSpeed = attackSpeed;
        this.maxHit = maxHit;
        this.intelligentPathing = intelligentPathing;
    }

    /**
     * Creates a modified copy with a different attack range.
     *
     * @param attackRange attack range.
     * @return copied profile.
     */
    public SimulationNpcProfile withAttackRange(int attackRange) {
        return new SimulationNpcProfile(attackRange, attackStyle, attackSpeed, maxHit, intelligentPathing);
    }

    /**
     * Creates a modified copy with a different attack style.
     *
     * @param attackStyle attack style.
     * @return copied profile.
     */
    public SimulationNpcProfile withAttackStyle(NpcAttackStyle attackStyle) {
        return new SimulationNpcProfile(attackRange, attackStyle, attackSpeed, maxHit, intelligentPathing);
    }

    /**
     * Creates a modified copy with a different attack speed.
     *
     * @param attackSpeed attack speed.
     * @return copied profile.
     */
    public SimulationNpcProfile withAttackSpeed(int attackSpeed) {
        return new SimulationNpcProfile(attackRange, attackStyle, attackSpeed, maxHit, intelligentPathing);
    }

    /**
     * Creates a modified copy with a different max hit.
     *
     * @param maxHit max hit.
     * @return copied profile.
     */
    public SimulationNpcProfile withMaxHit(int maxHit) {
        return new SimulationNpcProfile(attackRange, attackStyle, attackSpeed, maxHit, intelligentPathing);
    }

    /**
     * Creates a modified copy with intelligent pathing setting.
     *
     * @param intelligentPathing intelligent pathing value.
     * @return copied profile.
     */
    public SimulationNpcProfile withIntelligentPathing(boolean intelligentPathing) {
        return new SimulationNpcProfile(attackRange, attackStyle, attackSpeed, maxHit, intelligentPathing);
    }
}
