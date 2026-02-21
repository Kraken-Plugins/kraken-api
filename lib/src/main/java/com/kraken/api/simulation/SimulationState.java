package com.kraken.api.simulation;

import com.kraken.api.service.map.WorldPointService;
import lombok.Getter;
import net.runelite.api.Prayer;
import net.runelite.api.coords.WorldPoint;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Mutable branchable state used while expanding the simulation tree.
 */
public final class SimulationState {
    @Getter
    private final SimulationScenario scenario;
    @Getter
    private final SimulationSnapshot snapshot;
    @Getter
    private final int npcCount;

    private final int[] npcIndices;
    private final int[] npcIds;
    private final int[] npcSizes;
    private final SimulationNpcProfile[] npcProfiles;

    private final int[] npcPackedPoints;
    private final int[] npcAttackCooldowns;
    private final boolean[] npcActive;

    @Getter
    private int tick;
    private int playerPackedPoint;
    private int queuedMoveTargetPackedPoint;
    private boolean queuedMoveRun;
    @Getter
    private int playerHitpoints;
    @Getter
    private int playerMaxHitpoints;
    @Getter
    private Prayer activeProtectionPrayer;
    @Getter
    private SimulationAction lastAppliedAction;

    private final Map<Integer, Integer> inventoryItemCounts;
    private final Set<Integer> equippedItemIds;
    private final Map<Integer, Integer> foodHealingByItemId;

    static SimulationState fromScenario(SimulationScenario scenario) {
        if (scenario == null) {
            throw new IllegalArgumentException("scenario cannot be null");
        }

        SimulationSnapshot snapshot = scenario.getSnapshot();
        List<SimulationNpcSnapshot> npcs = snapshot.getNpcs();
        int count = npcs.size();

        int[] npcIndices = new int[count];
        int[] npcIds = new int[count];
        int[] npcSizes = new int[count];
        SimulationNpcProfile[] npcProfiles = new SimulationNpcProfile[count];
        int[] npcPackedPoints = new int[count];
        int[] npcAttackCooldowns = new int[count];
        boolean[] npcActive = new boolean[count];

        for (int i = 0; i < count; i++) {
            SimulationNpcSnapshot npc = npcs.get(i);
            npcIndices[i] = npc.getIndex();
            npcIds[i] = npc.getId();
            npcSizes[i] = npc.getSize();
            npcProfiles[i] = scenario.resolveNpcProfile(npc.getId());
            npcPackedPoints[i] = npc.getPackedWorldPoint();
            npcAttackCooldowns[i] = 0;
            npcActive[i] = true;
        }

        SimulationPlayerSnapshot player = snapshot.getPlayer();

        return new SimulationState(
                scenario,
                snapshot,
                count,
                npcIndices,
                npcIds,
                npcSizes,
                npcProfiles,
                npcPackedPoints,
                npcAttackCooldowns,
                npcActive,
                snapshot.getGameTick(),
                player.getPackedWorldPoint(),
                -1,
                false,
                player.getHitpoints(),
                Math.max(1, player.getMaxHitpoints()),
                player.getActiveProtectionPrayer(),
                null,
                toMutableMap(player.getInventoryItemQuantities()),
                toMutableSet(player.getEquippedItemIds()),
                toMutableMap(player.getFoodHealingByItemId())
        );
    }

    private SimulationState(
            SimulationScenario scenario,
            SimulationSnapshot snapshot,
            int npcCount,
            int[] npcIndices,
            int[] npcIds,
            int[] npcSizes,
            SimulationNpcProfile[] npcProfiles,
            int[] npcPackedPoints,
            int[] npcAttackCooldowns,
            boolean[] npcActive,
            int tick,
            int playerPackedPoint,
            int queuedMoveTargetPackedPoint,
            boolean queuedMoveRun,
            int playerHitpoints,
            int playerMaxHitpoints,
            Prayer activeProtectionPrayer,
            SimulationAction lastAppliedAction,
            Map<Integer, Integer> inventoryItemCounts,
            Set<Integer> equippedItemIds,
            Map<Integer, Integer> foodHealingByItemId
    ) {
        this.scenario = scenario;
        this.snapshot = snapshot;
        this.npcCount = npcCount;
        this.npcIndices = npcIndices;
        this.npcIds = npcIds;
        this.npcSizes = npcSizes;
        this.npcProfiles = npcProfiles;
        this.npcPackedPoints = npcPackedPoints;
        this.npcAttackCooldowns = npcAttackCooldowns;
        this.npcActive = npcActive;
        this.tick = tick;
        this.playerPackedPoint = playerPackedPoint;
        this.queuedMoveTargetPackedPoint = queuedMoveTargetPackedPoint;
        this.queuedMoveRun = queuedMoveRun;
        this.playerMaxHitpoints = Math.max(1, playerMaxHitpoints);
        this.playerHitpoints = Math.max(0, Math.min(playerHitpoints, this.playerMaxHitpoints));
        this.activeProtectionPrayer = activeProtectionPrayer;
        this.lastAppliedAction = lastAppliedAction;
        this.inventoryItemCounts = inventoryItemCounts;
        this.equippedItemIds = equippedItemIds;
        this.foodHealingByItemId = foodHealingByItemId;
    }

    /**
     * Creates a branch-safe copy.
     *
     * @return copied state.
     */
    public SimulationState copy() {
        return new SimulationState(
                scenario,
                snapshot,
                npcCount,
                npcIndices,
                npcIds,
                npcSizes,
                npcProfiles,
                npcPackedPoints.clone(),
                npcAttackCooldowns.clone(),
                npcActive.clone(),
                tick,
                playerPackedPoint,
                queuedMoveTargetPackedPoint,
                queuedMoveRun,
                playerHitpoints,
                playerMaxHitpoints,
                activeProtectionPrayer,
                lastAppliedAction,
                new HashMap<>(inventoryItemCounts),
                new HashSet<>(equippedItemIds),
                new HashMap<>(foodHealingByItemId)
        );
    }

    /**
     * @return player world point.
     */
    public WorldPoint getPlayerWorldPoint() {
        return WorldPointService.unpack(playerPackedPoint);
    }

    /**
     * @return packed player world point.
     */
    public int getPlayerPackedPoint() {
        return playerPackedPoint;
    }

    /**
     * @return player world x.
     */
    public int getPlayerX() {
        return WorldPointService.getPackedX(playerPackedPoint);
    }

    /**
     * @return player world y.
     */
    public int getPlayerY() {
        return WorldPointService.getPackedY(playerPackedPoint);
    }

    /**
     * @return true when a movement destination is queued.
     */
    public boolean hasQueuedMovement() {
        return queuedMoveTargetPackedPoint >= 0;
    }

    /**
     * @return queued movement destination, or null when none.
     */
    public WorldPoint getQueuedMovementDestination() {
        if (queuedMoveTargetPackedPoint < 0) {
            return null;
        }
        return WorldPointService.unpack(queuedMoveTargetPackedPoint);
    }

    /**
     * @return true when queued movement is simulated as run speed.
     */
    public boolean isQueuedMovementRun() {
        return queuedMoveRun;
    }

    /**
     * @param prayer overhead prayer.
     */
    public void setActiveProtectionPrayer(Prayer prayer) {
        this.activeProtectionPrayer = prayer;
    }

    /**
     * Applies damage to player hitpoints.
     *
     * @param amount damage amount.
     */
    public void damagePlayer(int amount) {
        if (amount <= 0) {
            return;
        }
        playerHitpoints = Math.max(0, playerHitpoints - amount);
    }

    /**
     * Heals player hitpoints.
     *
     * @param amount heal amount.
     */
    public void healPlayer(int amount) {
        if (amount <= 0) {
            return;
        }
        playerHitpoints = Math.min(playerMaxHitpoints, playerHitpoints + amount);
    }

    /**
     * @param npcSlot internal npc slot.
     * @return RuneLite npc index.
     */
    public int getNpcIndex(int npcSlot) {
        assertNpcSlot(npcSlot);
        return npcIndices[npcSlot];
    }

    /**
     * @param npcSlot internal npc slot.
     * @return RuneLite npc id.
     */
    public int getNpcId(int npcSlot) {
        assertNpcSlot(npcSlot);
        return npcIds[npcSlot];
    }

    /**
     * @param npcSlot internal npc slot.
     * @return npc size.
     */
    public int getNpcSize(int npcSlot) {
        assertNpcSlot(npcSlot);
        return npcSizes[npcSlot];
    }

    /**
     * @param npcSlot internal npc slot.
     * @return npc profile resolved from scenario mapping.
     */
    public SimulationNpcProfile getNpcProfile(int npcSlot) {
        assertNpcSlot(npcSlot);
        return npcProfiles[npcSlot];
    }

    /**
     * @param npcSlot internal npc slot.
     * @return true when npc is active.
     */
    public boolean isNpcActive(int npcSlot) {
        assertNpcSlot(npcSlot);
        return npcActive[npcSlot];
    }

    /**
     * Sets npc active status.
     *
     * @param npcSlot internal npc slot.
     * @param active active state.
     */
    public void setNpcActive(int npcSlot, boolean active) {
        assertNpcSlot(npcSlot);
        npcActive[npcSlot] = active;
    }

    /**
     * @param npcSlot internal npc slot.
     * @return npc world point.
     */
    public WorldPoint getNpcWorldPoint(int npcSlot) {
        assertNpcSlot(npcSlot);
        return WorldPointService.unpack(npcPackedPoints[npcSlot]);
    }

    /**
     * @param npcSlot internal npc slot.
     * @return npc world x.
     */
    public int getNpcX(int npcSlot) {
        assertNpcSlot(npcSlot);
        return WorldPointService.getPackedX(npcPackedPoints[npcSlot]);
    }

    /**
     * @param npcSlot internal npc slot.
     * @return npc world y.
     */
    public int getNpcY(int npcSlot) {
        assertNpcSlot(npcSlot);
        return WorldPointService.getPackedY(npcPackedPoints[npcSlot]);
    }

    /**
     * @param npcSlot internal npc slot.
     * @return npc packed world point.
     */
    public int getNpcPackedPoint(int npcSlot) {
        assertNpcSlot(npcSlot);
        return npcPackedPoints[npcSlot];
    }

    /**
     * @param npcSlot internal npc slot.
     * @return npc attack cooldown in ticks.
     */
    public int getNpcAttackCooldown(int npcSlot) {
        assertNpcSlot(npcSlot);
        return npcAttackCooldowns[npcSlot];
    }

    /**
     * Sets npc attack cooldown.
     *
     * @param npcSlot internal npc slot.
     * @param cooldown cooldown in ticks.
     */
    public void setNpcAttackCooldown(int npcSlot, int cooldown) {
        assertNpcSlot(npcSlot);
        npcAttackCooldowns[npcSlot] = Math.max(0, cooldown);
    }

    /**
     * Finds a simulation npc slot from RuneLite npc index.
     *
     * @param npcIndex npc index.
     * @return slot index, or -1 when not found.
     */
    public int findNpcSlotByIndex(int npcIndex) {
        for (int i = 0; i < npcCount; i++) {
            if (npcIndices[i] == npcIndex) {
                return i;
            }
        }
        return -1;
    }

    /**
     * @return immutable inventory counts by item id.
     */
    public Map<Integer, Integer> getInventoryItemCounts() {
        return Collections.unmodifiableMap(inventoryItemCounts);
    }

    /**
     * @return immutable equipped item id set.
     */
    public Set<Integer> getEquippedItemIds() {
        return Collections.unmodifiableSet(equippedItemIds);
    }

    /**
     * @return immutable food healing map.
     */
    public Map<Integer, Integer> getFoodHealingByItemId() {
        return Collections.unmodifiableMap(foodHealingByItemId);
    }

    /**
     * @param itemId item id.
     * @return true when inventory contains the item.
     */
    public boolean hasInventoryItem(int itemId) {
        return getInventoryItemCount(itemId) > 0;
    }

    /**
     * @param itemId item id.
     * @return inventory quantity.
     */
    public int getInventoryItemCount(int itemId) {
        return inventoryItemCounts.getOrDefault(itemId, 0);
    }

    /**
     * Consumes one inventory item.
     *
     * @param itemId item id.
     * @return true when consumed.
     */
    public boolean consumeInventoryItem(int itemId) {
        int current = getInventoryItemCount(itemId);
        if (current <= 0) {
            return false;
        }
        if (current == 1) {
            inventoryItemCounts.remove(itemId);
        } else {
            inventoryItemCounts.put(itemId, current - 1);
        }
        return true;
    }

    /**
     * Adds an item to inventory counts.
     *
     * @param itemId item id.
     */
    public void addInventoryItem(int itemId) {
        if (itemId < 0) {
            return;
        }
        inventoryItemCounts.merge(itemId, 1, Integer::sum);
    }

    /**
     * @param itemId item id.
     * @return true when item is equipped.
     */
    public boolean isItemEquipped(int itemId) {
        return equippedItemIds.contains(itemId);
    }

    /**
     * Equips an item from inventory.
     *
     * @param itemId item id.
     * @return true when equipped.
     */
    public boolean equipItemFromInventory(int itemId) {
        if (!consumeInventoryItem(itemId)) {
            return false;
        }
        equippedItemIds.add(itemId);
        return true;
    }

    /**
     * @param itemId item id.
     * @return configured heal amount, or 0.
     */
    public int getFoodHealAmount(int itemId) {
        return foodHealingByItemId.getOrDefault(itemId, 0);
    }

    void setPlayerPackedPoint(int playerPackedPoint) {
        this.playerPackedPoint = playerPackedPoint;
    }

    void queueMovement(int targetPackedPoint, boolean run) {
        this.queuedMoveTargetPackedPoint = targetPackedPoint;
        this.queuedMoveRun = run;
    }

    int getQueuedMoveTargetPackedPoint() {
        return queuedMoveTargetPackedPoint;
    }

    void clearQueuedMovement() {
        this.queuedMoveTargetPackedPoint = -1;
        this.queuedMoveRun = false;
    }

    void setNpcPackedPoint(int npcSlot, int npcPackedPoint) {
        assertNpcSlot(npcSlot);
        this.npcPackedPoints[npcSlot] = npcPackedPoint;
    }

    void incrementTick() {
        tick++;
    }

    void setLastAppliedAction(SimulationAction action) {
        this.lastAppliedAction = action;
    }

    private void assertNpcSlot(int npcSlot) {
        if (npcSlot < 0 || npcSlot >= npcCount) {
            throw new IndexOutOfBoundsException("Invalid npc slot: " + npcSlot);
        }
    }

    private static Map<Integer, Integer> toMutableMap(Map<Integer, Integer> source) {
        if (source == null || source.isEmpty()) {
            return new HashMap<>();
        }
        return new HashMap<>(source);
    }

    private static Set<Integer> toMutableSet(Set<Integer> source) {
        if (source == null || source.isEmpty()) {
            return new HashSet<>();
        }
        return new HashSet<>(source);
    }
}
