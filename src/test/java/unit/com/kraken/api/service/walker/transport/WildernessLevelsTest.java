package unit.com.kraken.api.service.walker.transport;

import com.kraken.api.service.walker.transport.WildernessLevels;
import net.runelite.api.coords.WorldPoint;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Covers the wilderness bucket the walker reports to transport requirement checks.
 *
 * <p>A y-only formula treats anything north of 3520 as wilderness. Lunar Isle and Wintertodt sit
 * there and are not, so a teleport the planner accepted would then fail as
 * {@code TRANSPORT_REQUIREMENTS_UNMET}. These points are the ones that would have been wrong.</p>
 */
class WildernessLevelsTest {

    @Test
    void lunarIsleIsNotWilderness() {
        assertEquals(0, WildernessLevels.of(new WorldPoint(2130, 3915, 0)));
    }

    @Test
    void wintertodtCampIsNotWilderness() {
        assertEquals(0, WildernessLevels.of(new WorldPoint(1632, 3944, 0)));
    }

    @Test
    void waterbirthDungeonIsNotWilderness() {
        assertEquals(0, WildernessLevels.of(new WorldPoint(2450, 10140, 0)));
    }

    @Test
    void feroxEnclaveIsNotWilderness() {
        assertEquals(0, WildernessLevels.of(new WorldPoint(3130, 3630, 0)));
    }

    @Test
    void edgevilleIsNotWilderness() {
        assertEquals(0, WildernessLevels.of(new WorldPoint(3080, 3490, 0)));
    }

    @Test
    void justInsideTheDitchIsTheLevel20Bucket() {
        assertEquals(20, WildernessLevels.of(new WorldPoint(3100, 3526, 0)));
    }

    @Test
    void level20IsTheLevel30Bucket() {
        assertEquals(30, WildernessLevels.of(new WorldPoint(3100, 3680, 0)));
    }

    @Test
    void level30IsDeepWilderness() {
        assertEquals(31, WildernessLevels.of(new WorldPoint(3100, 3760, 0)));
    }

    @Test
    void aNullTileIsOutside() {
        assertEquals(0, WildernessLevels.of(null));
    }
}
