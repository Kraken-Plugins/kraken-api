package com.kraken.api.simulation;

import lombok.Getter;
import lombok.NonNull;
import net.runelite.api.coords.WorldPoint;

/**
 * Immutable snapshot of a RuneLite NPC at a single game tick.
 */
@Getter
public final class SimulationNpcSnapshot {
    private static final int DEFAULT_ATTACK_SPEED = 4;

    private final int index;
    private final int id;
    private final String name;
    private final WorldPoint worldPoint;
    private final int size;
    private final int attackRange;
    private final int attackSpeed;
    private final int maxHit;
    private final NpcAttackStyle attackStyle;
    private final boolean collidable;
    private final boolean stopWhenPlayerInLineOfSight;

    /**
     * Creates a snapshot model for a single NPC.
     *
     * @param index RuneLite NPC index for runtime lookup.
     * @param id RuneLite NPC id.
     * @param name NPC display name.
     * @param worldPoint NPC world tile (south-west anchor for multi-tile NPCs).
     * @param size NPC tile footprint.
     * @param attackRange NPC attack/line-of-sight range used by simulation.
     * @param collidable true when movement overlap checks should treat this NPC as blocking.
     * @param stopWhenPlayerInLineOfSight true when movement should stop once LoS to player is available.
     */
    public SimulationNpcSnapshot(
            int index,
            int id,
            String name,
            @NonNull WorldPoint worldPoint,
            int size,
            int attackRange,
            boolean collidable,
            boolean stopWhenPlayerInLineOfSight
    ) {
        this(
                index,
                id,
                name,
                worldPoint,
                size,
                attackRange,
                DEFAULT_ATTACK_SPEED,
                attackRange > 1 ? NpcAttackStyle.RANGED : NpcAttackStyle.MELEE,
                0,
                collidable,
                stopWhenPlayerInLineOfSight
        );
    }

    /**
     * Creates a snapshot model for a single NPC including combat metadata.
     *
     * @param index RuneLite NPC index for runtime lookup.
     * @param id RuneLite NPC id.
     * @param name NPC display name.
     * @param worldPoint NPC world tile (south-west anchor for multi-tile NPCs).
     * @param size NPC tile footprint.
     * @param attackRange NPC attack/line-of-sight range used by simulation.
     * @param attackSpeed NPC attack speed in game ticks.
     * @param attackStyle NPC attack style used for prayer matching.
     * @param maxHit simulated max hit for incoming damage modeling.
     * @param collidable true when movement overlap checks should treat this NPC as blocking.
     * @param stopWhenPlayerInLineOfSight true when movement should stop once LoS to player is available.
     */
    public SimulationNpcSnapshot(
            int index,
            int id,
            String name,
            @NonNull WorldPoint worldPoint,
            int size,
            int attackRange,
            int attackSpeed,
            NpcAttackStyle attackStyle,
            int maxHit,
            boolean collidable,
            boolean stopWhenPlayerInLineOfSight
    ) {
        if (size <= 0) {
            throw new IllegalArgumentException("size must be > 0");
        }
        if (attackRange <= 0) {
            throw new IllegalArgumentException("attackRange must be > 0");
        }
        if (attackSpeed <= 0) {
            throw new IllegalArgumentException("attackSpeed must be > 0");
        }
        if (maxHit < 0) {
            throw new IllegalArgumentException("maxHit must be >= 0");
        }

        this.index = index;
        this.id = id;
        this.name = name == null ? "" : name;
        this.worldPoint = worldPoint;
        this.size = size;
        this.attackRange = attackRange;
        this.attackSpeed = attackSpeed;
        this.attackStyle = attackStyle == null ? NpcAttackStyle.UNKNOWN : attackStyle;
        this.maxHit = maxHit;
        this.collidable = collidable;
        this.stopWhenPlayerInLineOfSight = stopWhenPlayerInLineOfSight;
    }

}
