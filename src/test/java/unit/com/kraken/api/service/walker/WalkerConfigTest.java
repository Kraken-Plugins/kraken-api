package unit.com.kraken.api.service.walker;

import com.kraken.api.service.pathfinding.GlobalPathfinderConfig;
import com.kraken.api.service.walker.WalkerConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the walker's default planning options.
 */
class WalkerConfigTest {

    @Test
    void aDefaultWalkDoesNotPlanUnimplementedTransports() {
        GlobalPathfinderConfig config = WalkerConfig.builder().build().getPathfinderConfig();

        assertFalse(config.isUseCanoes());
        assertFalse(config.isUseTeleportationMinigames());
    }

    @Test
    void thePathfinderLibraryStillDefaultsThoseKindsOn() {
        GlobalPathfinderConfig config = GlobalPathfinderConfig.builder().build();

        assertTrue(config.isUseCanoes());
        assertTrue(config.isUseTeleportationMinigames());
    }
}
