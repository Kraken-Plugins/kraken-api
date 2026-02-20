package com.kraken.api.simulation;

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
 * Mutable simulation state optimized for rapid cloning and stepping.
 */
public final class SimulationState {
    @Getter
    private final SimulationSnapshot snapshot;
    @Getter
    private final int npcCount;

    private final int[] npcIndices;
    private final int[] npcIds;
    private final int[] npcSizes;
    private final int[] npcAttackRanges;
    private final int[] npcAttackSpeeds;
    private final int[] npcAttackCooldowns;
    private final int[] npcMaxHits;
    private final NpcAttackStyle[] npcAttackStyles;
    private final boolean[] npcCollidable;
    private final boolean[] npcStopWhenLineOfSight;

    @Getter
    private int tick;
    @Getter
    private int playerX;
    @Getter
    private int playerY;
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

    private final int[] npcX;
    private final int[] npcY;
    private final boolean[] npcActive;

    static SimulationState fromSnapshot(SimulationSnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("snapshot cannot be null");
        }

        List<SimulationNpcSnapshot> npcs = snapshot.getNpcs();
        int count = npcs.size();

        int[] npcIndices = new int[count];
        int[] npcIds = new int[count];
        int[] npcSizes = new int[count];
        int[] npcAttackRanges = new int[count];
        int[] npcAttackSpeeds = new int[count];
        int[] npcAttackCooldowns = new int[count];
        int[] npcMaxHits = new int[count];
        NpcAttackStyle[] npcAttackStyles = new NpcAttackStyle[count];
        boolean[] npcCollidable = new boolean[count];
        boolean[] npcStopWhenLos = new boolean[count];
        int[] npcX = new int[count];
        int[] npcY = new int[count];
        boolean[] npcActive = new boolean[count];

        for (int i = 0; i < count; i++) {
            SimulationNpcSnapshot npc = npcs.get(i);
            npcIndices[i] = npc.getIndex();
            npcIds[i] = npc.getId();
            npcSizes[i] = npc.getSize();
            npcAttackRanges[i] = npc.getAttackRange();
            npcAttackSpeeds[i] = npc.getAttackSpeed();
            npcAttackCooldowns[i] = 0;
            npcAttackStyles[i] = npc.getAttackStyle();
            npcMaxHits[i] = npc.getMaxHit();
            npcCollidable[i] = npc.isCollidable();
            npcStopWhenLos[i] = npc.isStopWhenPlayerInLineOfSight();
            npcX[i] = npc.getWorldPoint().getX();
            npcY[i] = npc.getWorldPoint().getY();
            npcActive[i] = true;
        }

        SimulationPlayerSnapshot playerSnapshot = snapshot.getPlayer();

        return new SimulationState(
                snapshot,
                snapshot.getGameTick(),
                snapshot.getPlayerWorldPoint().getX(),
                snapshot.getPlayerWorldPoint().getY(),
                playerSnapshot.getHitpoints(),
                playerSnapshot.getMaxHitpoints(),
                playerSnapshot.getActiveProtectionPrayer(),
                count,
                npcIndices,
                npcIds,
                npcSizes,
                npcAttackRanges,
                npcAttackSpeeds,
                npcAttackCooldowns,
                npcMaxHits,
                npcAttackStyles,
                npcCollidable,
                npcStopWhenLos,
                toMutableMap(playerSnapshot.getInventoryItemQuantities()),
                toMutableSet(playerSnapshot.getEquippedItemIds()),
                toMutableMap(playerSnapshot.getFoodHealingByItemId()),
                npcX,
                npcY,
                npcActive,
                null
        );
    }

    private SimulationState(
            SimulationSnapshot snapshot,
            int tick,
            int playerX,
            int playerY,
            int playerHitpoints,
            int playerMaxHitpoints,
            Prayer activeProtectionPrayer,
            int npcCount,
            int[] npcIndices,
            int[] npcIds,
            int[] npcSizes,
            int[] npcAttackRanges,
            int[] npcAttackSpeeds,
            int[] npcAttackCooldowns,
            int[] npcMaxHits,
            NpcAttackStyle[] npcAttackStyles,
            boolean[] npcCollidable,
            boolean[] npcStopWhenLineOfSight,
            Map<Integer, Integer> inventoryItemCounts,
            Set<Integer> equippedItemIds,
            Map<Integer, Integer> foodHealingByItemId,
            int[] npcX,
            int[] npcY,
            boolean[] npcActive,
            SimulationAction lastAppliedAction
    ) {
        this.snapshot = snapshot;
        this.tick = tick;
        this.playerX = playerX;
        this.playerY = playerY;
        this.playerMaxHitpoints = Math.max(1, playerMaxHitpoints);
        this.playerHitpoints = Math.max(0, Math.min(playerHitpoints, this.playerMaxHitpoints));
        this.activeProtectionPrayer = activeProtectionPrayer;
        this.npcCount = npcCount;
        this.npcIndices = npcIndices;
        this.npcIds = npcIds;
        this.npcSizes = npcSizes;
        this.npcAttackRanges = npcAttackRanges;
        this.npcAttackSpeeds = npcAttackSpeeds;
        this.npcAttackCooldowns = npcAttackCooldowns;
        this.npcMaxHits = npcMaxHits;
        this.npcAttackStyles = npcAttackStyles;
        this.npcCollidable = npcCollidable;
        this.npcStopWhenLineOfSight = npcStopWhenLineOfSight;
        this.inventoryItemCounts = inventoryItemCounts;
        this.equippedItemIds = equippedItemIds;
        this.foodHealingByItemId = foodHealingByItemId;
        this.npcX = npcX;
        this.npcY = npcY;
        this.npcActive = npcActive;
        this.lastAppliedAction = lastAppliedAction;
    }

    /**
     * Creates a branch-safe copy of this state.
     *
     * <p>Static snapshot metadata is shared; mutable entity arrays are copied.</p>
     *
     * @return deep-enough copy for decision-tree expansion.
     */
    public SimulationState copy() {
        return new SimulationState(
                snapshot,
                tick,
                playerX,
                playerY,
                playerHitpoints,
                playerMaxHitpoints,
                activeProtectionPrayer,
                npcCount,
                npcIndices,
                npcIds,
                npcSizes,
                npcAttackRanges,
                npcAttackSpeeds,
                npcAttackCooldowns.clone(),
                npcMaxHits,
                npcAttackStyles,
                npcCollidable,
                npcStopWhenLineOfSight,
                new HashMap<>(inventoryItemCounts),
                new HashSet<>(equippedItemIds),
                new HashMap<>(foodHealingByItemId),
                npcX.clone(),
                npcY.clone(),
                npcActive.clone(),
                lastAppliedAction
        );
    }

    /**
     * Internal tick increment used by {@link SimulationEngine}.
     */
    void incrementTick() {
        tick++;
    }

    /**
     * @return player world position.
     */
    public WorldPoint getPlayerWorldPoint() {
        return new WorldPoint(playerX, playerY, snapshot.getPlane());
    }

    /**
     * Internal player position mutator used during simulation stepping.
     */
    void setPlayerPosition(int worldX, int worldY) {
        playerX = worldX;
        playerY = worldY;
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
     * Heals player hitpoints by an amount, capped by max hitpoints.
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
     * Sets active overhead protection prayer for this state.
     *
     * @param prayer overhead prayer.
     */
    public void setActiveProtectionPrayer(Prayer prayer) {
        this.activeProtectionPrayer = prayer;
    }

    /**
     * Sets the most recent action applied to this state.
     *
     * @param action last action.
     */
    void setLastAppliedAction(SimulationAction action) {
        this.lastAppliedAction = action;
    }

    /**
     * Returns captured NPC index for a simulation slot.
     *
     * @param npcSlot internal slot index.
     * @return RuneLite NPC index.
     */
    public int getNpcIndex(int npcSlot) {
        assertNpcSlot(npcSlot);
        return npcIndices[npcSlot];
    }

    /**
     * @param npcSlot internal slot index.
     * @return RuneLite NPC id.
     */
    public int getNpcId(int npcSlot) {
        assertNpcSlot(npcSlot);
        return npcIds[npcSlot];
    }

    /**
     * @param npcSlot internal slot index.
     * @return NPC tile footprint size.
     */
    public int getNpcSize(int npcSlot) {
        assertNpcSlot(npcSlot);
        return npcSizes[npcSlot];
    }

    /**
     * @param npcSlot internal slot index.
     * @return NPC attack/LoS range used by simulation.
     */
    public int getNpcAttackRange(int npcSlot) {
        assertNpcSlot(npcSlot);
        return npcAttackRanges[npcSlot];
    }

    /**
     * @param npcSlot internal slot index.
     * @return NPC attack style.
     */
    public NpcAttackStyle getNpcAttackStyle(int npcSlot) {
        assertNpcSlot(npcSlot);
        return npcAttackStyles[npcSlot];
    }

    /**
     * @param npcSlot internal slot index.
     * @return NPC attack speed in ticks.
     */
    public int getNpcAttackSpeed(int npcSlot) {
        assertNpcSlot(npcSlot);
        return npcAttackSpeeds[npcSlot];
    }

    /**
     * @param npcSlot internal slot index.
     * @return ticks until this npc can attack.
     */
    public int getNpcAttackCooldown(int npcSlot) {
        assertNpcSlot(npcSlot);
        return npcAttackCooldowns[npcSlot];
    }

    /**
     * Sets ticks until this NPC can attack again.
     *
     * @param npcSlot internal slot index.
     * @param cooldown ticks remaining.
     */
    public void setNpcAttackCooldown(int npcSlot, int cooldown) {
        assertNpcSlot(npcSlot);
        npcAttackCooldowns[npcSlot] = Math.max(0, cooldown);
    }

    /**
     * @param npcSlot internal slot index.
     * @return NPC simulated max hit.
     */
    public int getNpcMaxHit(int npcSlot) {
        assertNpcSlot(npcSlot);
        return npcMaxHits[npcSlot];
    }

    /**
     * @param npcSlot internal slot index.
     * @return true when NPC overlap should block movement.
     */
    public boolean isNpcCollidable(int npcSlot) {
        assertNpcSlot(npcSlot);
        return npcCollidable[npcSlot];
    }

    /**
     * @param npcSlot internal slot index.
     * @return true when NPC movement should stop after gaining player LoS.
     */
    public boolean isNpcStopWhenLineOfSight(int npcSlot) {
        assertNpcSlot(npcSlot);
        return npcStopWhenLineOfSight[npcSlot];
    }

    /**
     * @param npcSlot internal slot index.
     * @return true when the NPC is active in this state.
     */
    public boolean isNpcActive(int npcSlot) {
        assertNpcSlot(npcSlot);
        return npcActive[npcSlot];
    }

    /**
     * Enables or disables an NPC slot.
     *
     * @param npcSlot internal slot index.
     * @param active active flag.
     */
    public void setNpcActive(int npcSlot, boolean active) {
        assertNpcSlot(npcSlot);
        npcActive[npcSlot] = active;
    }

    /**
     * @param npcSlot internal slot index.
     * @return NPC world x coordinate.
     */
    public int getNpcX(int npcSlot) {
        assertNpcSlot(npcSlot);
        return npcX[npcSlot];
    }

    /**
     * @param npcSlot internal slot index.
     * @return NPC world y coordinate.
     */
    public int getNpcY(int npcSlot) {
        assertNpcSlot(npcSlot);
        return npcY[npcSlot];
    }

    /**
     * @param npcSlot internal slot index.
     * @return NPC world point.
     */
    public WorldPoint getNpcWorldPoint(int npcSlot) {
        assertNpcSlot(npcSlot);
        return new WorldPoint(npcX[npcSlot], npcY[npcSlot], snapshot.getPlane());
    }

    /**
     * Internal NPC position mutator used during simulation stepping.
     */
    void setNpcPosition(int npcSlot, int worldX, int worldY) {
        assertNpcSlot(npcSlot);
        npcX[npcSlot] = worldX;
        npcY[npcSlot] = worldY;
    }

    /**
     * Resolves an internal NPC slot by RuneLite NPC index.
     *
     * @param npcIndex RuneLite NPC index.
     * @return internal slot index, or {@code -1} when not present.
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
     * @return immutable view of inventory item stacks by item id.
     */
    public Map<Integer, Integer> getInventoryItemCounts() {
        return Collections.unmodifiableMap(inventoryItemCounts);
    }

    /**
     * @return immutable view of equipped item ids.
     */
    public Set<Integer> getEquippedItemIds() {
        return Collections.unmodifiableSet(equippedItemIds);
    }

    /**
     * @return immutable view of configured food heal mapping by item id.
     */
    public Map<Integer, Integer> getFoodHealingByItemId() {
        return Collections.unmodifiableMap(foodHealingByItemId);
    }

    /**
     * @param itemId item id.
     * @return true when inventory has one or more of item id.
     */
    public boolean hasInventoryItem(int itemId) {
        return getInventoryItemCount(itemId) > 0;
    }

    /**
     * @param itemId item id.
     * @return inventory stack count for item id.
     */
    public int getInventoryItemCount(int itemId) {
        return inventoryItemCounts.getOrDefault(itemId, 0);
    }

    /**
     * Consumes one inventory item when present.
     *
     * @param itemId item id.
     * @return true when one item was consumed.
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
     * Adds one item to inventory stacks.
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
     * @return true when item is currently equipped.
     */
    public boolean isItemEquipped(int itemId) {
        return equippedItemIds.contains(itemId);
    }

    /**
     * Equips an item from inventory.
     *
     * @param itemId item id.
     * @return true when item transitioned from inventory to equipped.
     */
    public boolean equipItemFromInventory(int itemId) {
        if (!consumeInventoryItem(itemId)) {
            return false;
        }
        equippedItemIds.add(itemId);
        return true;
    }

    /**
     * Resolves configured heal amount for a food item id.
     *
     * @param itemId food item id.
     * @return heal amount, or {@code 0}.
     */
    public int getFoodHealAmount(int itemId) {
        return foodHealingByItemId.getOrDefault(itemId, 0);
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
