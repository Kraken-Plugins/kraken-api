package unit.com.kraken.api.service.walker;

import com.kraken.api.service.magic.spellbook.Ancient;
import com.kraken.api.service.magic.spellbook.Arceuus;
import com.kraken.api.service.magic.spellbook.Lunar;
import com.kraken.api.service.magic.spellbook.Standard;
import com.kraken.api.service.walker.HomeTeleportPlan;
import net.runelite.api.coords.WorldPoint;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers when a Lumbridge home teleport is a better first step than walking.
 *
 * <p>The final choice is dense path length, not 2D to the destination. Underground tiles sit at
 * {@code y + 6400}, which made Lumbridge look closer than a volcano the player is already on.</p>
 */
class HomeTeleportPlanTest {

    private static final WorldPoint FARM = new WorldPoint(3241, 3282, 0);
    private static final WorldPoint BANK = new WorldPoint(3208, 3218, 2);
    private static final WorldPoint GE = new WorldPoint(3164, 3486, 0);
    private static final WorldPoint MUSA_POINT = new WorldPoint(2901, 3161, 0);

    @Test
    void theFarmIsFarEnoughFromTheCourtyardAndTheBankIsNot() {
        assertTrue(HomeTeleportPlan.isFarFromCourtyard(FARM));
        assertFalse(HomeTeleportPlan.isFarFromCourtyard(HomeTeleportPlan.COURTYARD));
        assertFalse(HomeTeleportPlan.isFarFromCourtyard(BANK));
    }

    @Test
    void theCourtyardIsCloserToTheCastleBankThanTheFarmIs() {
        assertTrue(HomeTeleportPlan.courtyardIsCloser(FARM, BANK));
        assertFalse(HomeTeleportPlan.courtyardIsCloser(FARM, GE));
        assertFalse(HomeTeleportPlan.courtyardIsCloser(BANK, GE));
    }

    @Test
    void farmToCastleBankPrefersTheHomeTeleport() {
        assertTrue(HomeTeleportPlan.preferViaCourtyard(FARM, true, 80, true, 20));
    }

    @Test
    void aLongerWalkToTheBankStillPrefersTheHomeTeleport() {
        WorldPoint alKharidMine = new WorldPoint(3261, 3334, 0);

        assertTrue(HomeTeleportPlan.isFarFromCourtyard(alKharidMine));
        assertTrue(HomeTeleportPlan.courtyardIsCloser(alKharidMine, BANK));
        assertTrue(HomeTeleportPlan.preferViaCourtyard(alKharidMine, true, 80, true, 20));
    }

    @Test
    void aShorterWalkFromHereDoesNotSpendTheCooldown() {
        assertFalse(HomeTeleportPlan.preferViaCourtyard(FARM, true, 180, true, 220));
    }

    @Test
    void musaPointToTheKaramjaDungeonDoesNotHomeTeleport() {
        WorldPoint dungeon = new WorldPoint(2856, 9574, 0);

        assertTrue(HomeTeleportPlan.isFarFromCourtyard(MUSA_POINT));
        assertTrue(HomeTeleportPlan.courtyardIsCloser(MUSA_POINT, dungeon));
        assertFalse(HomeTeleportPlan.preferViaCourtyard(MUSA_POINT, true, 50, true, 400));
    }

    @Test
    void anIncompleteWalkStillTeleportsWhenTheCourtyardRouteFinishes() {
        assertTrue(HomeTeleportPlan.preferViaCourtyard(FARM, false, 0, true, 20));
    }

    @Test
    void anIncompleteCourtyardRouteIsNotPreferred() {
        assertFalse(HomeTeleportPlan.preferViaCourtyard(FARM, true, 80, false, 0));
    }

    @Test
    void standingNearTheCourtyardDoesNotTeleport() {
        assertFalse(HomeTeleportPlan.preferViaCourtyard(HomeTeleportPlan.COURTYARD, true, 80, true, 20));
    }

    @Test
    void allBooksHomeTeleportsShareTheTimer() {
        assertTrue(HomeTeleportPlan.isHomeTeleport(Standard.HOME_TELEPORT));
        assertTrue(HomeTeleportPlan.isHomeTeleport(Lunar.LUNAR_HOME_TELEPORT));
        assertTrue(HomeTeleportPlan.isHomeTeleport(Ancient.EDGEVILLE_HOME_TELEPORT));
        assertTrue(HomeTeleportPlan.isHomeTeleport(Arceuus.ARCEUUS_HOME_TELEPORT));
        assertFalse(HomeTeleportPlan.isHomeTeleport(Standard.LUMBRIDGE_TELEPORT));
        assertFalse(HomeTeleportPlan.isHomeTeleport(null));
    }

    @Test
    void aZeroTimerIsNotOnCooldown() {
        assertFalse(HomeTeleportPlan.isOnCooldown(0, Instant.now()));
    }

    @Test
    void aRecentTimerIsOnCooldown() {
        int minutes = (int) (Instant.now().getEpochSecond() / 60);
        assertTrue(HomeTeleportPlan.isOnCooldown(minutes, Instant.now()));
        assertFalse(HomeTeleportPlan.isOnCooldown(minutes, Instant.now().plus(31, ChronoUnit.MINUTES)));
    }
}
