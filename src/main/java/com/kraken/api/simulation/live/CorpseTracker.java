package com.kraken.api.simulation.live;

import com.kraken.api.simulation.data.NpcDef;
import com.kraken.api.simulation.world.Frame;
import com.kraken.api.simulation.world.SimState;

import java.util.Arrays;

/**
 * Corpse capture: a dead NPC still in the scene is a body that blocks other NPCs until it is deleted, the type's
 * corpse ticks after the killing hit. The first capture that sees it dead stands in for the killing tick.
 */
public final class CorpseTracker {
    private final int[] deathTick = new int[SimState.MAX_NPCS];

    /** Creates an empty tracker; no slot has a body sighted. */
    public CorpseTracker() {
        reset();
    }

    /**
     * Whether a dead NPC of this type leaves a blocking body worth capturing.
     * @param def the type
     * @return false for body less types and types removed at once
     */
    public static boolean leavesBody(NpcDef def) {
        return !def.noBody && def.corpseTicks > 0;
    }

    /**
     * Ticks until a dead slot's body is deleted, stamping its first dead sighting.
     * @param slot the slot
     * @param def the slot's type
     * @param clientTick the capture's client tick
     * @return the countdown to capture, at least 1
     */
    public int corpseTicks(int slot, NpcDef def, int clientTick) {
        if (deathTick[slot] == Integer.MIN_VALUE) {
            deathTick[slot] = clientTick;
        }
        return Math.max(1, def.corpseTicks - (clientTick - deathTick[slot]));
    }

    /**
     * Writes a dead NPC into the frame and state as a corpse: typed and indexed like the living, no hp, not alive,
     * its deletion countdown set.
     * @param frame the frame being captured
     * @param state the state being captured
     * @param slot the slot
     * @param typeIdx the type index
     * @param npcIndex the game's NPC index
     * @param pos the packed region local SW tile
     * @param homePos the slot's home tile, or -1
     * @param spawnTick the slot's spawn tick ({@code SimState.SPAWN_OPEN} when unknown)
     * @param clientTick the capture's client tick
     */
    public void capture(Frame frame, SimState state, int slot, int typeIdx, int npcIndex, int pos, int homePos,
                        int spawnTick, int clientTick) {
        NpcDef def = frame.data.npcs[typeIdx];
        frame.npcTypeIdx[slot] = (byte) typeIdx;
        frame.npcIndex[slot] = npcIndex;
        frame.npcMaxHp[slot] = (short) def.hp;
        frame.npcHomePos[slot] = (short) homePos;
        state.npcPos[slot] = (short) pos;
        state.npcHp[slot] = 0;
        state.npcFlags[slot] = 0;
        state.npcSpawnTick[slot] = (short) spawnTick;
        state.npcCorpse[slot] = (byte) corpseTicks(slot, def, clientTick);
    }

    /**
     * Forgets a slot's death.
     * @param slot the slot
     */
    public void clearSlot(int slot) {
        deathTick[slot] = Integer.MIN_VALUE;
    }

    /** Forgets every death. */
    public void reset() {
        Arrays.fill(deathTick, Integer.MIN_VALUE);
    }
}
