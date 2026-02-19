package com.kraken.api.simulation;

import lombok.Getter;
import lombok.NonNull;
import net.runelite.api.coords.WorldPoint;

/**
 * Immutable snapshot of a RuneLite NPC at a single game tick.
 */
@Getter
public final class SimulationNpcSnapshot {
    private final int index;
    private final int id;
    private final String name;
    private final WorldPoint worldPoint;
    private final int size;
    private final int attackRange;
    private final boolean collidable;
    private final boolean stopWhenPlayerInLineOfSight;

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
        if (size <= 0) {
            throw new IllegalArgumentException("size must be > 0");
        }
        if (attackRange <= 0) {
            throw new IllegalArgumentException("attackRange must be > 0");
        }

        this.index = index;
        this.id = id;
        this.name = name == null ? "" : name;
        this.worldPoint = worldPoint;
        this.size = size;
        this.attackRange = attackRange;
        this.collidable = collidable;
        this.stopWhenPlayerInLineOfSight = stopWhenPlayerInLineOfSight;
    }

}
