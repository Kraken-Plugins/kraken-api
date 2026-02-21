package com.kraken.api.simulation.snapshot;

import lombok.Getter;
import lombok.NonNull;
import net.runelite.api.coords.WorldPoint;
import com.kraken.api.service.map.WorldPointService;

/**
 * Immutable NPC position snapshot used as simulation input.
 */
@Getter
public final class SimulationNpcSnapshot {
    private final int index;
    private final int id;
    private final int size;
    private final int packedWorldPoint;
    private final String name;

    /**
     * Creates an npc snapshot.
     *
     * @param index RuneLite npc index.
     * @param id RuneLite npc id.
     * @param size npc size in tiles.
     * @param worldPoint npc anchor tile.
     */
    public SimulationNpcSnapshot(int index, int id, int size, @NonNull WorldPoint worldPoint) {
        this(index, id, size, WorldPointService.pack(worldPoint), null);
    }

    /**
     * Creates an npc snapshot with packed coordinates.
     *
     * @param index RuneLite npc index.
     * @param id RuneLite npc id.
     * @param size npc size in tiles.
     * @param packedWorldPoint packed npc world point.
     * @param name optional npc name.
     */
    public SimulationNpcSnapshot(int index, int id, int size, int packedWorldPoint, String name) {
        if (size <= 0) {
            throw new IllegalArgumentException("size must be > 0");
        }
        this.index = index;
        this.id = id;
        this.size = size;
        this.packedWorldPoint = packedWorldPoint;
        this.name = name == null ? "" : name;
    }

    /**
     * @return unpacked world point.
     */
    public WorldPoint getWorldPoint() {
        return WorldPointService.unpack(packedWorldPoint);
    }
}
