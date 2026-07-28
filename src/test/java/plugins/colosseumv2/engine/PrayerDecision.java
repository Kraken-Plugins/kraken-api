package plugins.colosseumv2.engine;

import lombok.Value;
import net.runelite.api.Prayer;

/**
 * The engine's output for one tick: the protection prayer that must be active on the NEXT tick
 * (activated now, with one tick remaining), or {@code null} when no prayer is required.
 */
@Value
public class PrayerDecision {

    public enum Reason {
        NONE,
        QUEUED_ATTACK,
        PRE_PRAY
    }

    public static final PrayerDecision NONE = new PrayerDecision(null, Reason.NONE, null, 0);

    Prayer prayer;
    Reason reason;

    /** The queue entry that won conflict resolution, when {@link #reason} is QUEUED_ATTACK. */
    QueuedAttack winner;

    /** Number of same-tick candidates that competed for the next tick. */
    int contested;
}
