package com.kraken.api.simulation.npc;

import com.kraken.api.simulation.tick.TickContext;

/**
 * A content pack's per type NPC mechanics, called by the engine at fixed points of each NPC's turn; a pack extends this
 * class once per behaviour and overrides the hooks it needs. Behaviours are
 * stateless: everything they keep lives in the state's lanes ({@code SimState.npcLane}, {@code FLAG_PACK_*}), so a
 * tick still allocates nothing and a state copy carries it. One instance serves every NPC of its type; it may hold
 * the type's immutable settings.
 *
 * <p>The engine calls only the hooks a behaviour overrides (it reads them once when the pack binds), so a type with no
 * behaviour, or one overriding only {@link #attack}, pays nothing for the others. The order within one NPC's turn:
 *
 * <pre>
 * dead:   corpse countdown and deletion (core), then {@link #onDeadTurn}
 * living: {@link #onTurnStart} (may end the turn), cooldown tick down and spawn gates (core),
 *         {@link #turn} (may end the turn), else the core's movement and sight, then {@link #attack}
 * </pre>
 *
 * <p>Hooks are virtual calls the JIT cannot inline once several behaviours share a call site, so a behaviour that
 * owns its whole turn does it in one {@link #turn} rather than a movement hook and an attack hook.
 */
public abstract class NpcBehavior {
    /** Hook bit for {@link #onDeadTurn}. */
    public static final int HOOK_DEAD_TURN = 1;
    /** Hook bit for {@link #onTurnStart}. */
    public static final int HOOK_TURN_START = 2;
    /** Hook bit for {@link #turn}. */
    public static final int HOOK_TURN = 4;
    /** Hook bit for {@link #attack}. */
    public static final int HOOK_ATTACK = 8;
    /** Hook bit for {@link #onDeath}. */
    public static final int HOOK_DEATH = 16;

    /**
     * A dead NPC's turn, after the core counted its corpse down (respawn clocks).
     * @param c the tick's context
     * @param slot the NPC's slot
     */
    public void onDeadTurn(TickContext c, int slot) {
    }

    /**
     * Start of a living NPC's turn, before its cooldown ticks down (crawl clocks, heal casts).
     * @param c the tick's context
     * @param slot the NPC's slot
     * @return true to end the turn here: no cooldown, gates, movement or attack this tick
     */
    public boolean onTurnStart(TickContext c, int slot) {
        return false;
    }

    /**
     * The NPC's own movement and attack, after its cooldown ticked down and its spawn gates were read. Called on
     * every turn the NPC takes, gates open or not ({@link TickContext#canMove()}, {@link TickContext#canAttack()}).
     * Returning true ends the turn: the core does none of its movement, sight check, sight exposure count or
     * {@link #attack}. A behaviour that only replaces the attack overrides {@link #attack} instead.
     * @param c the tick's context
     * @param slot the NPC's slot
     * @return true when the behaviour took the turn, false for the core's movement and then {@link #attack}
     */
    public boolean turn(TickContext c, int slot) {
        return false;
    }

    /**
     * This tick's attack decision and launch, after movement. The default is the core's standard attack: one styled
     * hit of the type's rolls and hit delay rule when the attack gate is open, the NPC has reach and sight, and its
     * cooldown has run out ({@link TickContext#standardAttack}).
     * @param c the tick's context
     * @param slot the NPC's slot
     */
    public void attack(TickContext c, int slot) {
        c.standardAttack(slot);
    }

    /**
     * The NPC was killed. The core has already cleared it alive, credited a kill of an NPC that holds the wave open,
     * and dropped the player's target; the corpse countdown starts after this hook.
     * @param c the tick's context
     * @param slot the NPC's slot
     */
    public void onDeath(TickContext c, int slot) {
    }
}
