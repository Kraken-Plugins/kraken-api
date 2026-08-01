package plugins.colosseum.simulation;

import lombok.Getter;

/**
 * Immutable per-decision context shared by every {@link State} branched from one
 * snapshot: the collision grid, per-slot NPC identity data and the player loadout.
 *
 * <p>Splitting the immutable identity data out of {@link State} keeps state copies down
 * to a handful of small primitive-array copies; the frame itself is shared by reference
 * across the whole search tree.</p>
 */
@Getter
public final class Frame {
    private final Grid grid;
    private final LoadoutConfig loadout;

    private final int npcSlotCount;
    /** Per-slot colosseum type. */
    private final NpcType[] npcTypes;
    /** Per-slot RuneLite npc index for mapping decisions back onto live NPCs. */
    private final int[] npcRuneliteIndices;
    /** Per-slot max hitpoints (from type, kept separately to allow modifier scaling). */
    private final short[] npcMaxHp;
    /**
     * Slot indices in tick-processing order (sorted by type processing priority, then
     * RuneLite index), matching the validated community simulator ordering.
     */
    private final byte[] processingOrder;

    private final int playerMaxHp;
    private final int prayerPointCap;

    /** True when wave-start gating applies (state tick 0 is the wave-start tick). */
    private final boolean waveStartGates;
    /** Warband members attack when {@code tick % 6 == warbandCyclePhase}. */
    private final int warbandCyclePhase;
    /** Mantimayhem modifier tier 0-3 (3 allows melee-first manticore patterns). */
    private final int mantimayhemTier;

    /**
     * Creates a frame.
     *
     * @param grid collision grid.
     * @param loadout player loadout.
     * @param npcTypes per-slot npc types.
     * @param npcRuneliteIndices per-slot RuneLite npc indices.
     * @param npcMaxHp per-slot max hitpoints.
     * @param playerMaxHp player max hitpoints.
     * @param prayerPointCap player max prayer points.
     * @param waveStartGates true when tick 0 is the wave start (movement/LoS/attack gating).
     * @param warbandCyclePhase warband 6-tick cycle phase.
     * @param mantimayhemTier mantimayhem modifier tier, 0 when absent.
     */
    public Frame(
            Grid grid,
            LoadoutConfig loadout,
            NpcType[] npcTypes,
            int[] npcRuneliteIndices,
            short[] npcMaxHp,
            int playerMaxHp,
            int prayerPointCap,
            boolean waveStartGates,
            int warbandCyclePhase,
            int mantimayhemTier
    ) {
        if (grid == null || loadout == null || npcTypes == null || npcRuneliteIndices == null || npcMaxHp == null) {
            throw new IllegalArgumentException("frame inputs cannot be null");
        }
        if (npcTypes.length != npcRuneliteIndices.length || npcTypes.length != npcMaxHp.length) {
            throw new IllegalArgumentException("npc arrays must have equal length");
        }
        if (npcTypes.length > Constants.MAX_NPCS) {
            throw new IllegalArgumentException("too many npc slots: " + npcTypes.length);
        }
        this.grid = grid;
        this.loadout = loadout;
        this.npcSlotCount = npcTypes.length;
        this.npcTypes = npcTypes.clone();
        this.npcRuneliteIndices = npcRuneliteIndices.clone();
        this.npcMaxHp = npcMaxHp.clone();
        this.playerMaxHp = Math.max(1, playerMaxHp);
        this.prayerPointCap = Math.max(1, prayerPointCap);
        this.waveStartGates = waveStartGates;
        this.warbandCyclePhase = Math.floorMod(warbandCyclePhase, Constants.WARBAND_CYCLE_TICKS);
        this.mantimayhemTier = mantimayhemTier;
        this.processingOrder = buildProcessingOrder(this.npcTypes, this.npcRuneliteIndices);
    }

    /**
     * @param slot npc slot.
     * @return npc type for the slot.
     */
    public NpcType type(int slot) {
        return npcTypes[slot];
    }

    /**
     * @param slot npc slot.
     * @return npc footprint size for the slot.
     */
    public int size(int slot) {
        return npcTypes[slot].getSize();
    }

    /**
     * Finds a slot by RuneLite npc index.
     *
     * @param runeliteIndex RuneLite npc index.
     * @return slot, or -1 when untracked.
     */
    public int slotByRuneliteIndex(int runeliteIndex) {
        for (int i = 0; i < npcSlotCount; i++) {
            if (npcRuneliteIndices[i] == runeliteIndex) {
                return i;
            }
        }
        return -1;
    }

    private static byte[] buildProcessingOrder(NpcType[] types, int[] runeliteIndices) {
        Integer[] slots = new Integer[types.length];
        for (int i = 0; i < slots.length; i++) {
            slots[i] = i;
        }
        java.util.Arrays.sort(slots, (a, b) -> {
            int byPriority = Integer.compare(types[a].getProcessingPriority(), types[b].getProcessingPriority());
            if (byPriority != 0) {
                return byPriority;
            }
            return Integer.compare(runeliteIndices[a], runeliteIndices[b]);
        });
        byte[] order = new byte[slots.length];
        for (int i = 0; i < slots.length; i++) {
            order[i] = (byte) (int) slots[i];
        }
        return order;
    }
}
