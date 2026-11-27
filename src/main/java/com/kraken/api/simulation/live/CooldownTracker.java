package com.kraken.api.simulation.live;

import com.kraken.api.simulation.world.SimState;

import java.util.Arrays;

/**
 * Attack cadence tracking from the animation stream ({@link AnimationStamper} feeds it). The launch tick is stamped
 * as the animation tick minus the pipeline's {@code attackAnimationLagTicks}: by the time the animation is reactable
 * the damage and prayer check are already resolved, so prayers must be dispatched at least one tick ahead of the
 * predicted animation, and the lag shifts every animation anchored stamp if resolution comes earlier still.
 *
 * <p>Each stamp also records the attack's projectile delay and whether the player's overhead blocked it at launch,
 * so capture can re seed still in flight hits into the state's pending ring.
 */
public final class CooldownTracker {
    private final int[] launchTick = new int[SimState.MAX_NPCS];
    private final byte[] launchDelay = new byte[SimState.MAX_NPCS];
    private final byte[] launchStyle = new byte[SimState.MAX_NPCS];
    private final boolean[] launchBlocked = new boolean[SimState.MAX_NPCS];

    private int lastPlayerAttackTick = Integer.MIN_VALUE;

    /** Creates an empty tracker; every slot reads unstamped. */
    public CooldownTracker() {
        reset();
    }

    /**
     * Records an NPC attack launch observed as an animation.
     * @param slot the attacker's slot
     * @param animationTick the client tick of the animation
     * @param lagTicks ticks the real launch precedes the animation
     * @param delayTicks in flight ticks for this attack (its hit delay at launch range)
     * @param style the attack's style, or -1 when unknown
     * @param blocked the overhead active at launch fully blocks it (never re seeded)
     */
    public void stamp(int slot, int animationTick, int lagTicks, int delayTicks, int style, boolean blocked) {
        launchTick[slot] = animationTick - lagTicks;
        launchDelay[slot] = (byte) delayTicks;
        launchStyle[slot] = (byte) style;
        launchBlocked[slot] = blocked;
    }

    /**
     * Records the player's attack animation.
     * @param animationTick the client tick of the animation
     */
    public void stampPlayerAttack(int animationTick) {
        lastPlayerAttackTick = animationTick;
    }

    /**
     * The tick of a slot's last launch.
     * @param slot the slot
     * @return the tick, or Integer.MIN_VALUE when never stamped
     */
    public int lastLaunch(int slot) {
        return launchTick[slot];
    }

    /**
     * Ticks until the slot's next attack as the engine models cooldowns (decremented at the start of the NPC's turn,
     * firing at 0 or less). An unstamped slot reads ready: the conservative default, the model predicts the attack no
     * later than it can happen.
     * @param slot the slot
     * @param attackSpeed the type's attack speed
     * @param clientTick the capture's client tick
     * @return the cooldown to capture, -1 to attackSpeed
     */
    public int cooldownAt(int slot, int attackSpeed, int clientTick) {
        if (launchTick[slot] == Integer.MIN_VALUE) {
            return 0;
        }
        return clamp(attackSpeed - (clientTick - launchTick[slot]), -1, attackSpeed);
    }

    /**
     * The player's attack check runs before the end of tick decrement (the opposite of NPC ordering), so a captured
     * value of speed minus elapsed minus 1 re attacks exactly {@code attackSpeed} ticks after the stamp.
     * @param attackSpeed the weapon's attack speed
     * @param clientTick the capture's client tick
     * @return the player's cooldown to capture, -1 to attackSpeed
     */
    public int playerCooldownAt(int attackSpeed, int clientTick) {
        if (lastPlayerAttackTick == Integer.MIN_VALUE) {
            return 0;
        }
        return clamp(attackSpeed - (clientTick - lastPlayerAttackTick) - 1, -1, attackSpeed);
    }

    /**
     * Remaining in flight ticks of the slot's last launched attack.
     * @param slot the slot
     * @param clientTick the capture's client tick
     * @return the ticks, or -1 when nothing from it is still in the air (landed, blocked, or never seen)
     */
    public int inFlightRemaining(int slot, int clientTick) {
        return inFlightRemaining(slot, clientTick, false);
    }

    /**
     * As {@link #inFlightRemaining(int, int)}, optionally counting a launch the overhead blocked (an attack that must
     * also be prayed at landing).
     * @param slot the slot
     * @param clientTick the capture's client tick
     * @param includeBlocked whether a blocked launch counts
     * @return the ticks, or -1 when nothing counts as in the air
     */
    public int inFlightRemaining(int slot, int clientTick, boolean includeBlocked) {
        if (launchTick[slot] == Integer.MIN_VALUE || (launchBlocked[slot] && !includeBlocked)) {
            return -1;
        }
        int remaining = launchDelay[slot] - (clientTick - launchTick[slot]);
        return remaining >= 1 ? remaining : -1;
    }

    /**
     * The style of the slot's last launch.
     * @param slot the slot
     * @return the style, or -1 when unknown
     */
    public int inFlightStyle(int slot) {
        return launchStyle[slot];
    }

    /**
     * Whether the overhead blocked the slot's last launch.
     * @param slot the slot
     * @return true when blocked
     */
    public boolean inFlightBlocked(int slot) {
        return launchBlocked[slot];
    }

    /**
     * Forgets a slot's stamp.
     * @param slot the slot
     */
    public void clearSlot(int slot) {
        launchTick[slot] = Integer.MIN_VALUE;
        launchDelay[slot] = 0;
        launchStyle[slot] = -1;
        launchBlocked[slot] = false;
    }

    /** Forgets every stamp. */
    public void reset() {
        Arrays.fill(launchTick, Integer.MIN_VALUE);
        Arrays.fill(launchDelay, (byte) 0);
        Arrays.fill(launchStyle, (byte) -1);
        Arrays.fill(launchBlocked, false);
        lastPlayerAttackTick = Integer.MIN_VALUE;
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
