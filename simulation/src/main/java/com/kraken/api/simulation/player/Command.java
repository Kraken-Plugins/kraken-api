package com.kraken.api.simulation.player;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * One tick of player intent packed into a long. {@code NONE} keeps doing what previous ticks set up (the path
 * continues, engaged combat re attacks on cooldown).
 *
 * <p>Layout: [0-11] move destination, [12] move flag, [15-17] overhead, [18-20] gear set, [21-24] consumable bits
 * (food, karambwan, brew, restore), [25-29] attack, [30] spec arm. Attack and move are mutually exclusive per tick:
 * a new click replaces the current interaction.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Command {
    /** No intent: the path and engaged attack of previous ticks carry on. */
    public static final long NONE = 0L;

    /** Leave the overhead as it is. */
    public static final int OVERHEAD_KEEP = 0;
    /** Turn the overhead off. */
    public static final int OVERHEAD_OFF = 1;
    /** Switch to Protect from Melee. */
    public static final int OVERHEAD_MELEE = 2;
    /** Switch to Protect from Missiles. */
    public static final int OVERHEAD_MISSILES = 3;
    /** Switch to Protect from Magic. */
    public static final int OVERHEAD_MAGIC = 4;

    /** Leave the attack target as it is. */
    public static final int ATTACK_KEEP = 0;
    /** Added to an NPC slot to encode an attack click, so the lowest codes stay free for no attack. */
    public static final int ATTACK_SLOT_BASE = 2;

    /** Consumable bit: eat food. */
    public static final int CONSUME_FOOD = 1;
    /** Consumable bit: eat a karambwan. */
    public static final int CONSUME_KARAMBWAN = 2;
    /** Consumable bit: drink a brew dose. */
    public static final int CONSUME_BREW = 4;
    /** Consumable bit: drink a prayer restore dose. */
    public static final int CONSUME_RESTORE = 8;

    /**
     * A move click.
     * @param destPos the packed destination
     * @return the command bits
     */
    public static long move(int destPos) {
        return destPos | 1L << 12;
    }

    /**
     * An overhead change.
     * @param code one of the {@code OVERHEAD_*} codes
     * @return the command bits
     */
    public static long overhead(int code) {
        return (long) code << 15;
    }

    /**
     * A gear switch.
     * @param set the gear set index, 0 to 3
     * @return the command bits
     */
    public static long gearSet(int set) {
        return (long) (set + 1) << 18;
    }

    /**
     * An attack click.
     * @param slot the target slot
     * @return the command bits
     */
    public static long attack(int slot) {
        return (long) (ATTACK_SLOT_BASE + slot) << 25;
    }

    /**
     * Consumables taken this tick.
     * @param bits {@code CONSUME_*} bits
     * @return the command bits
     */
    public static long consume(int bits) {
        return (long) bits << 21;
    }

    /**
     * Arms the special attack.
     * @return the command bits
     */
    public static long spec() {
        return 1L << 30;
    }

    /**
     * Whether a command clicks a tile.
     * @param cmd the command
     * @return true for a move
     */
    public static boolean hasMove(long cmd) {
        return (cmd >>> 12 & 1) != 0;
    }

    /**
     * A move's destination.
     * @param cmd the command
     * @return the packed tile
     */
    public static int dest(long cmd) {
        return (int) (cmd & 0xfff);
    }

    /**
     * The overhead code.
     * @param cmd the command
     * @return one of the {@code OVERHEAD_*} codes
     */
    public static int overheadOf(long cmd) {
        return (int) (cmd >>> 15 & 7);
    }

    /**
     * The gear field.
     * @param cmd the command
     * @return 0 to keep, else the set index plus one
     */
    public static int gearSetOf(long cmd) {
        return (int) (cmd >>> 18 & 7);
    }

    /**
     * The attack field.
     * @param cmd the command
     * @return {@link #ATTACK_KEEP}, else the slot plus {@link #ATTACK_SLOT_BASE}
     */
    public static int attackOf(long cmd) {
        return (int) (cmd >>> 25 & 31);
    }

    /**
     * The consumable bits.
     * @param cmd the command
     * @return {@code CONSUME_*} bits
     */
    public static int consumablesOf(long cmd) {
        return (int) (cmd >>> 21 & 15);
    }

    /**
     * Whether the command arms the special attack.
     * @param cmd the command
     * @return true when armed
     */
    public static boolean specOf(long cmd) {
        return (cmd >>> 30 & 1) != 0;
    }
}
