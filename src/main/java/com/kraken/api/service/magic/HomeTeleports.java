package com.kraken.api.service.magic;

import com.kraken.api.service.magic.spellbook.Ancient;
import com.kraken.api.service.magic.spellbook.Arceuus;
import com.kraken.api.service.magic.spellbook.Lunar;
import com.kraken.api.service.magic.spellbook.Standard;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Shared home-teleport facts: which spells they are, and the thirty-minute cooldown.
 *
 * <p>Every book's home teleport shares {@code AIDE_TELE_TIMER}. The walker only <em>chooses</em> the
 * standard-book Lumbridge one, and it refuses that choice while this timer is running. CS2
 * {@code isCastable} does not consult the timer — home teleport has no runes — so a clickable-looking
 * spell can still be on cooldown. VitaLite uses the same varp check.</p>
 */
public final class HomeTeleports {

    private HomeTeleports() {
    }

    /**
     * Whether a spell is any book's home teleport.
     *
     * @param spell the spell, may be null
     * @return true when it is a home teleport
     */
    public static boolean isHomeTeleport(CastableSpell spell) {
        return spell == Standard.HOME_TELEPORT
                || spell == Lunar.LUNAR_HOME_TELEPORT
                || spell == Ancient.EDGEVILLE_HOME_TELEPORT
                || spell == Arceuus.ARCEUUS_HOME_TELEPORT;
    }

    /**
     * Whether the shared home-teleport timer is still running.
     *
     * @param aideTeleTimerMinutes the {@code AIDE_TELE_TIMER} varp, minutes since epoch of last use
     * @param now the current time, may be null
     * @return true when the spell cannot be cast yet
     */
    public static boolean isOnCooldown(int aideTeleTimerMinutes, Instant now) {
        if (now == null) {
            return false;
        }

        Instant lastUsed = Instant.ofEpochSecond((long) aideTeleTimerMinutes * 60L);
        return lastUsed.plus(30, ChronoUnit.MINUTES).isAfter(now);
    }
}
