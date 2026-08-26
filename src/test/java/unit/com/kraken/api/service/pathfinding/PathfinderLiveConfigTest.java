package unit.com.kraken.api.service.pathfinding;

import com.kraken.api.service.pathfinding.GlobalPathfinderConfig;
import com.kraken.api.service.pathfinding.PathfinderLiveConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers turning members-only transport types off on a free-to-play world.
 */
class PathfinderLiveConfigTest {

    @Test
    void aFreeToPlayWorldTurnsOffMembersTransportTypes() {
        GlobalPathfinderConfig resolved = PathfinderLiveConfig.resolve(
                GlobalPathfinderConfig.builder().build(), false);

        assertFalse(resolved.isUseAgilityShortcuts());
        assertFalse(resolved.isUseGrappleShortcuts());
        assertFalse(resolved.isUseCanoes());
        assertFalse(resolved.isUseCharterShips());
        assertFalse(resolved.isUseFairyRings());
        assertFalse(resolved.isUseGnomeGliders());
        assertFalse(resolved.isUseHotAirBalloons());
        assertFalse(resolved.isUseMagicCarpets());
        assertFalse(resolved.isUseMagicMushtrees());
        assertFalse(resolved.isUseMinecarts());
        assertFalse(resolved.isUseQuetzals());
        assertFalse(resolved.isUseSeasonalTransports());
        assertFalse(resolved.isUseSpiritTrees());
        assertFalse(resolved.isUseTeleportationBoxes());
        assertFalse(resolved.isUseTeleportationItems());
        assertFalse(resolved.isUseTeleportationMinigames());
        assertFalse(resolved.isUseTeleportationPortals());
        assertFalse(resolved.isUseTeleportationPortalsPoh());
        assertFalse(resolved.isUseWildernessObelisks());
    }

    @Test
    void aFreeToPlayWorldKeepsBoatsShipsAndSpells() {
        GlobalPathfinderConfig resolved = PathfinderLiveConfig.resolve(
                GlobalPathfinderConfig.builder().build(), false);

        assertTrue(resolved.isUseBoats());
        assertTrue(resolved.isUseShips());
        assertTrue(resolved.isUseTeleportationSpells());
    }

    @Test
    void aMembersWorldLeavesTheCallerFlagsAlone() {
        GlobalPathfinderConfig requested = GlobalPathfinderConfig.builder()
                .useAgilityShortcuts(true)
                .useCanoes(true)
                .build();

        GlobalPathfinderConfig resolved = PathfinderLiveConfig.resolve(requested, true);

        assertTrue(resolved.isUseAgilityShortcuts());
        assertTrue(resolved.isUseCanoes());
    }

    @Test
    void anAlreadyDisabledTypeStaysDisabledOnFreeToPlay() {
        GlobalPathfinderConfig requested = GlobalPathfinderConfig.builder()
                .useBoats(false)
                .build();

        GlobalPathfinderConfig resolved = PathfinderLiveConfig.resolve(requested, false);

        assertFalse(resolved.isUseBoats());
    }
}
