package com.kraken.api.simulation;

import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

import java.util.List;

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
    private final boolean[] npcCollidable;
    private final boolean[] npcStopWhenLineOfSight;

    @Getter
    private int tick;
    @Getter
    private int playerX;
    @Getter
    private int playerY;

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
            npcCollidable[i] = npc.isCollidable();
            npcStopWhenLos[i] = npc.isStopWhenPlayerInLineOfSight();
            npcX[i] = npc.getWorldPoint().getX();
            npcY[i] = npc.getWorldPoint().getY();
            npcActive[i] = true;
        }

        return new SimulationState(
                snapshot,
                snapshot.getGameTick(),
                snapshot.getPlayerWorldPoint().getX(),
                snapshot.getPlayerWorldPoint().getY(),
                count,
                npcIndices,
                npcIds,
                npcSizes,
                npcAttackRanges,
                npcCollidable,
                npcStopWhenLos,
                npcX,
                npcY,
                npcActive
        );
    }

    private SimulationState(
            SimulationSnapshot snapshot,
            int tick,
            int playerX,
            int playerY,
            int npcCount,
            int[] npcIndices,
            int[] npcIds,
            int[] npcSizes,
            int[] npcAttackRanges,
            boolean[] npcCollidable,
            boolean[] npcStopWhenLineOfSight,
            int[] npcX,
            int[] npcY,
            boolean[] npcActive
    ) {
        this.snapshot = snapshot;
        this.tick = tick;
        this.playerX = playerX;
        this.playerY = playerY;
        this.npcCount = npcCount;
        this.npcIndices = npcIndices;
        this.npcIds = npcIds;
        this.npcSizes = npcSizes;
        this.npcAttackRanges = npcAttackRanges;
        this.npcCollidable = npcCollidable;
        this.npcStopWhenLineOfSight = npcStopWhenLineOfSight;
        this.npcX = npcX;
        this.npcY = npcY;
        this.npcActive = npcActive;
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
                npcCount,
                npcIndices,
                npcIds,
                npcSizes,
                npcAttackRanges,
                npcCollidable,
                npcStopWhenLineOfSight,
                npcX.clone(),
                npcY.clone(),
                npcActive.clone()
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

    private void assertNpcSlot(int npcSlot) {
        if (npcSlot < 0 || npcSlot >= npcCount) {
            throw new IndexOutOfBoundsException("Invalid npc slot: " + npcSlot);
        }
    }
}
