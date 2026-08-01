package plugins.colosseum.simulation;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * One tick's worth of player intent, bit-packed into a single {@code long} so search nodes
 * can branch over thousands of commands with zero allocation.
 *
 * <p>A command mirrors what a real player can queue in one tick: one movement click (or
 * stop), an overhead prayer switch, a gear set swap (any number of equips resolve in the
 * same tick), at most one food + one combo food + one potion, an attack target, and a
 * special attack flag. {@link #NONE} keeps everything as-is.</p>
 *
 * <pre>
 * bits 0-11  movement destination (packed local pos), 0xFFF = no new click
 * bit  12    run toggle for the movement click
 * bit  13    stop current movement
 * bits 14-16 overhead: 0 keep, 1 off, 2 protect melee, 3 protect missiles, 4 protect magic
 * bits 17-19 gear set: 0 keep, otherwise set index + 1
 * bit  20    eat primary food
 * bit  21    eat combo food (karambwan)
 * bit  22    sip Saradomin brew dose
 * bit  23    sip super restore dose
 * bit  24    use special attack on the next player attack
 * bits 25-30 attack target: 0 keep, 63 clear, otherwise npc slot + 1
 * </pre>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PlayerCommand {
    /** The empty command: keep doing whatever the previous ticks set up. */
    public static final long NONE = 0xFFFL;

    /** Overhead code for {@code keep current}. */
    public static final int OVERHEAD_KEEP = 0;
    /** Overhead code for {@code prayers off}. */
    public static final int OVERHEAD_OFF = 1;
    /** Overhead code for Protect from Melee. */
    public static final int OVERHEAD_MELEE = 2;
    /** Overhead code for Protect from Missiles. */
    public static final int OVERHEAD_MISSILES = 3;
    /** Overhead code for Protect from Magic. */
    public static final int OVERHEAD_MAGIC = 4;

    private static final long MOVE_MASK = 0xFFFL;
    private static final long RUN_BIT = 1L << 12;
    private static final long STOP_BIT = 1L << 13;
    private static final int OVERHEAD_SHIFT = 14;
    private static final int GEAR_SHIFT = 17;
    private static final long EAT_FOOD_BIT = 1L << 20;
    private static final long EAT_COMBO_BIT = 1L << 21;
    private static final long SIP_BREW_BIT = 1L << 22;
    private static final long SIP_RESTORE_BIT = 1L << 23;
    private static final long SPEC_BIT = 1L << 24;
    private static final int ATTACK_SHIFT = 25;
    private static final int ATTACK_CLEAR = 63;

    /**
     * @param dest packed local destination.
     * @param run true to run.
     * @return command moving to the destination.
     */
    public static long moveTo(short dest, boolean run) {
        long cmd = NONE & ~MOVE_MASK;
        cmd |= dest & MOVE_MASK;
        if (run) {
            cmd |= RUN_BIT;
        }
        return cmd;
    }

    /**
     * Merges a movement click into an existing command.
     *
     * @param cmd base command.
     * @param dest packed local destination.
     * @param run true to run.
     * @return combined command.
     */
    public static long withMove(long cmd, short dest, boolean run) {
        long result = (cmd & ~(MOVE_MASK | RUN_BIT | STOP_BIT)) | (dest & MOVE_MASK);
        if (run) {
            result |= RUN_BIT;
        }
        return result;
    }

    /**
     * @param cmd base command.
     * @return command that also stops current movement.
     */
    public static long withStop(long cmd) {
        return (cmd & ~MOVE_MASK) | NONE | STOP_BIT;
    }

    /**
     * @param cmd base command.
     * @param overheadCode one of the OVERHEAD_* codes.
     * @return combined command.
     */
    public static long withOverhead(long cmd, int overheadCode) {
        return (cmd & ~(7L << OVERHEAD_SHIFT)) | ((long) (overheadCode & 7) << OVERHEAD_SHIFT);
    }

    /**
     * @param cmd base command.
     * @param gearSetIndex gear set index to switch to.
     * @return combined command.
     */
    public static long withGearSet(long cmd, int gearSetIndex) {
        return (cmd & ~(7L << GEAR_SHIFT)) | ((long) ((gearSetIndex + 1) & 7) << GEAR_SHIFT);
    }

    /**
     * @param cmd base command.
     * @param food eat primary food.
     * @param combo eat combo food.
     * @param brew sip brew dose.
     * @param restore sip restore dose.
     * @return combined command.
     */
    public static long withConsumables(long cmd, boolean food, boolean combo, boolean brew, boolean restore) {
        long result = cmd & ~(EAT_FOOD_BIT | EAT_COMBO_BIT | SIP_BREW_BIT | SIP_RESTORE_BIT);
        if (food) {
            result |= EAT_FOOD_BIT;
        }
        if (combo) {
            result |= EAT_COMBO_BIT;
        }
        if (brew) {
            result |= SIP_BREW_BIT;
        }
        if (restore) {
            result |= SIP_RESTORE_BIT;
        }
        return result;
    }

    /**
     * @param cmd base command.
     * @param npcSlot npc slot to attack.
     * @return combined command.
     */
    public static long withAttack(long cmd, int npcSlot) {
        return (cmd & ~(63L << ATTACK_SHIFT)) | ((long) ((npcSlot + 1) & 63) << ATTACK_SHIFT);
    }

    /**
     * @param cmd base command.
     * @return command that clears the attack target.
     */
    public static long withAttackClear(long cmd) {
        return (cmd & ~(63L << ATTACK_SHIFT)) | ((long) ATTACK_CLEAR << ATTACK_SHIFT);
    }

    /**
     * @param cmd base command.
     * @return command with the special attack flag set.
     */
    public static long withSpec(long cmd) {
        return cmd | SPEC_BIT;
    }

    /** @param cmd command. @return packed move destination or {@link ColoCoords#NONE}.
     *  @return moveTarget        */
    public static short moveTarget(long cmd) {
        int raw = (int) (cmd & MOVE_MASK);
        return raw == 0xFFF ? ColoCoords.NONE : (short) raw;
    }

    /** @param cmd command. @return true when the movement click is a run click.
     *             @return run */
    public static boolean run(long cmd) {
        return (cmd & RUN_BIT) != 0;
    }

    /** @param cmd command. @return true when movement should stop.
     * @return stop */
    public static boolean stop(long cmd) {
        return (cmd & STOP_BIT) != 0;
    }

    /** @param cmd command. @return OVERHEAD_* code.
     *             @return overhead */
    public static int overhead(long cmd) {
        return (int) ((cmd >> OVERHEAD_SHIFT) & 7);
    }

    /** @param cmd command. @return gear set index, or -1 to keep the current set.
     *             @return gearSet */
    public static int gearSet(long cmd) {
        int raw = (int) ((cmd >> GEAR_SHIFT) & 7);
        return raw == 0 ? -1 : raw - 1;
    }

    /** @param cmd command. @return true when eating primary food.
     * @return eatFood */
    public static boolean eatFood(long cmd) {
        return (cmd & EAT_FOOD_BIT) != 0;
    }

    /** @param cmd command. @return true when eating combo food.
     *             @return  eatCombo */
    public static boolean eatCombo(long cmd) {
        return (cmd & EAT_COMBO_BIT) != 0;
    }

    /** @param cmd command. @return true when sipping a brew dose.
     *             @return sipBrew*/
    public static boolean sipBrew(long cmd) {
        return (cmd & SIP_BREW_BIT) != 0;
    }

    /** @param cmd command. @return true when sipping a restore dose.
     * @return sipRestore */
    public static boolean sipRestore(long cmd) {
        return (cmd & SIP_RESTORE_BIT) != 0;
    }

    /** @param cmd command. @return true when the special attack flag is set.
     *             @return useSpec */
    public static boolean useSpec(long cmd) {
        return (cmd & SPEC_BIT) != 0;
    }

    /**
     * @param cmd command.
     * @return npc slot to attack, -1 to keep the current target, -2 to clear it.
     */
    public static int attackTarget(long cmd) {
        int raw = (int) ((cmd >> ATTACK_SHIFT) & 63);
        if (raw == 0) {
            return -1;
        }
        if (raw == ATTACK_CLEAR) {
            return -2;
        }
        return raw - 1;
    }
}
