package com.kraken.api.simulation;

import com.kraken.api.service.map.WorldPointService;
import lombok.Getter;
import net.runelite.api.Prayer;
import net.runelite.api.coords.WorldPoint;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Immutable player metadata included in a simulation snapshot.
 */
@Getter
public final class SimulationPlayerSnapshot {
    private static final int DEFAULT_MAX_HP = 99;

    private final int packedWorldPoint;
    private final int hitpoints;
    private final int maxHitpoints;
    private final Prayer activeProtectionPrayer;
    private final Map<Integer, Integer> inventoryItemQuantities;
    private final Set<Integer> equippedItemIds;
    private final Map<Integer, Integer> foodHealingByItemId;

    /**
     * Creates immutable player combat/action metadata used by simulation actions.
     *
     * @param worldPoint player world point at capture time.
     * @param hitpoints player hitpoints at capture time.
     * @param maxHitpoints player max hitpoints at capture time.
     * @param activeProtectionPrayer active overhead protection prayer at capture time.
     * @param inventoryItemQuantities stack size by item id for inventory items.
     * @param equippedItemIds equipped item ids.
     * @param foodHealingByItemId configured heal amount by food item id.
     */
    public SimulationPlayerSnapshot(
            WorldPoint worldPoint,
            int hitpoints,
            int maxHitpoints,
            Prayer activeProtectionPrayer,
            Map<Integer, Integer> inventoryItemQuantities,
            Set<Integer> equippedItemIds,
            Map<Integer, Integer> foodHealingByItemId
    ) {
        if (worldPoint == null) {
            throw new IllegalArgumentException("worldPoint cannot be null");
        }
        if (maxHitpoints <= 0) {
            throw new IllegalArgumentException("maxHitpoints must be > 0");
        }
        if (hitpoints < 0) {
            throw new IllegalArgumentException("hitpoints must be >= 0");
        }

        this.packedWorldPoint = WorldPointService.pack(worldPoint);
        this.maxHitpoints = maxHitpoints;
        this.hitpoints = Math.min(hitpoints, maxHitpoints);
        this.activeProtectionPrayer = activeProtectionPrayer;
        this.inventoryItemQuantities = immutablePositiveCountMap(inventoryItemQuantities);
        this.equippedItemIds = immutablePositiveIdSet(equippedItemIds);
        this.foodHealingByItemId = immutablePositiveCountMap(foodHealingByItemId);
    }

    /**
     * @return default player snapshot when no explicit metadata is provided.
     */
    public static SimulationPlayerSnapshot empty(WorldPoint worldPoint) {
        return new SimulationPlayerSnapshot(
                worldPoint,
                DEFAULT_MAX_HP,
                DEFAULT_MAX_HP,
                null,
                Collections.emptyMap(),
                Collections.emptySet(),
                Collections.emptyMap()
        );
    }

    /**
     * Resolves configured food healing for a specific item id.
     *
     * @param itemId item id.
     * @return heal amount, or {@code 0} when not configured.
     */
    public int getFoodHealAmount(int itemId) {
        return foodHealingByItemId.getOrDefault(itemId, 0);
    }

    /**
     * @return unpacked player world point.
     */
    public WorldPoint getWorldPoint() {
        return WorldPointService.unpack(packedWorldPoint);
    }

    private static Map<Integer, Integer> immutablePositiveCountMap(Map<Integer, Integer> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Integer, Integer> normalized = new HashMap<>();
        for (Map.Entry<Integer, Integer> entry : source.entrySet()) {
            Integer key = entry.getKey();
            Integer value = entry.getValue();
            if (key == null || key < 0 || value == null || value <= 0) {
                continue;
            }
            normalized.merge(key, value, Integer::sum);
        }

        if (normalized.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(normalized);
    }

    private static Set<Integer> immutablePositiveIdSet(Set<Integer> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptySet();
        }

        Set<Integer> normalized = new HashSet<>();
        for (Integer id : source) {
            if (id != null && id >= 0) {
                normalized.add(id);
            }
        }

        if (normalized.isEmpty()) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(normalized);
    }
}
