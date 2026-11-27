package com.kraken.api.simulation.live;

import com.kraken.api.simulation.world.SimState;

import java.util.Arrays;

/**
 * Stable game NPC index to simulation slot mapping: an NPC keeps its slot for its lifetime so per slot tracker state
 * (hp ledger, cooldowns, a pack's observers) survives across ticks; slots free on despawn and recycle for new spawns.
 */
public final class NpcIndexer {
    private final int[] npcIndexBySlot = new int[SimState.MAX_NPCS];

    /** Creates an indexer with every slot free. */
    public NpcIndexer() {
        Arrays.fill(npcIndexBySlot, -1);
    }

    /** Frees every slot. */
    public void reset() {
        Arrays.fill(npcIndexBySlot, -1);
    }

    /**
     * The slot of an NPC.
     * @param npcIndex the game's NPC index
     * @return the slot, or -1 when it has none
     */
    public int slotOf(int npcIndex) {
        for (int i = 0; i < npcIndexBySlot.length; i++) {
            if (npcIndexBySlot[i] == npcIndex) {
                return i;
            }
        }
        return -1;
    }

    /**
     * The NPC's slot, assigning the first free one if it is new.
     * @param npcIndex the game's NPC index
     * @return the slot, or -1 when every slot is taken
     */
    public int slotForOrAssign(int npcIndex) {
        int slot = slotOf(npcIndex);
        if (slot >= 0) {
            return slot;
        }
        for (int i = 0; i < npcIndexBySlot.length; i++) {
            if (npcIndexBySlot[i] < 0) {
                npcIndexBySlot[i] = npcIndex;
                return i;
            }
        }
        return -1;
    }

    /**
     * Frees every slot not marked in the seen mask. The trackers keep their per slot state until told, so the caller
     * clears each freed slot on every tracker it uses ({@code CooldownTracker.clearSlot}, {@code HpLedger.clearSlot},
     * {@code CorpseTracker.clearSlot}); otherwise a new NPC given the slot inherits the old one's cooldown, damage and
     * death tick.
     * @param seenMask a bit per slot seen this capture
     * @return the freed slots' mask
     */
    public int releaseUnseen(int seenMask) {
        int freed = 0;
        for (int i = 0; i < npcIndexBySlot.length; i++) {
            if (npcIndexBySlot[i] >= 0 && (seenMask >> i & 1) == 0) {
                npcIndexBySlot[i] = -1;
                freed |= 1 << i;
            }
        }
        return freed;
    }
}
