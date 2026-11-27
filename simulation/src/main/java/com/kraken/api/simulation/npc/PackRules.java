package com.kraken.api.simulation.npc;

import com.kraken.api.simulation.tick.TickContext;

/**
 * A content pack's world scope mechanics: hazards, spawn schedules, and rules that span NPC types. Each hook runs at
 * a fixed point of the tick pipeline (see {@code Engine}); a pack extends this class once and overrides what it
 * needs. Like behaviours, rules keep their state in the state's lanes ({@code SimState.packLane}, {@code npcLane},
 * {@code accLane}).
 *
 * <p>Every hook is a virtual call the JIT may not inline into the tick, so the engine calls as few as it can: the per
 * tick and per hit hooks only when the frame's {@code packHooks} names them (all of them unless the pack narrows it
 * for a frame, for example when no modifier that steps a hazard is active), the rest only on the ticks that need
 * them.
 */
public abstract class PackRules {
    /** {@code Frame.packHooks} bit: call {@link #onTickStart}. */
    public static final int HOOK_TICK_START = 1;
    /** {@code Frame.packHooks} bit: call {@link #contact}. */
    public static final int HOOK_CONTACT = 2;
    /** {@code Frame.packHooks} bit: call {@link #onNpcDamaged}. */
    public static final int HOOK_NPC_DAMAGED = 4;
    /** {@code Frame.packHooks} bit: call {@link #endOfTick}. */
    public static final int HOOK_END_OF_TICK = 8;
    /** Every gated hook. */
    public static final int HOOKS_ALL = HOOK_TICK_START | HOOK_CONTACT | HOOK_NPC_DAMAGED | HOOK_END_OF_TICK;

    /** No world rules. */
    public static final PackRules NONE = new PackRules() {
    };

    /**
     * P0, after in flight hits aged and tile picks: moving hazards step.
     * @param c the tick's context
     */
    public void onTickStart(TickContext c) {
    }

    /**
     * End of P2, after every NPC took its turn, on ticks where a hook set {@link TickContext#tickScratch}: rules
     * across the NPCs of a tick (one volley per tick).
     * @param c the tick's context
     */
    public void afterNpcTurns(TickContext c) {
    }

    /**
     * A tile targeted hit landed on its tile this tick, before the core checks whether the player stood there.
     * @param c the tick's context
     * @param tile the packed tile
     */
    public void onTileHitLanded(TickContext c, int tile) {
    }

    /**
     * Ground hazard contact for the player's current tile, after the core's molten sand.
     * @param c the tick's context
     */
    public void contact(TickContext c) {
    }

    /**
     * A player hit landed on an NPC that survived it.
     * @param c the tick's context
     * @param slot the NPC's slot
     * @param damage the damage it took
     */
    public void onNpcDamaged(TickContext c, int slot, int damage) {
    }

    /**
     * A body was deleted (at once for a body less or instantly removed NPC, else when its corpse countdown ends on
     * its turn); its footprint is already off the board.
     * @param c the tick's context
     * @param slot the NPC's slot
     * @return a mask of slots this killed in turn, 0 for none
     */
    public int onBodyDeleted(TickContext c, int slot) {
        return 0;
    }

    /**
     * Last step of P4, after the core's accumulators and its wave clear check: the pack's own accumulators and its
     * scheduled spawns. The wave is open while {@code SimState.waveClearTick} is below zero; the core sets it on the
     * tick no NPC that holds the wave open is alive, so spawns due that tick never happen.
     * @param c the tick's context
     */
    public void endOfTick(TickContext c) {
    }
}
