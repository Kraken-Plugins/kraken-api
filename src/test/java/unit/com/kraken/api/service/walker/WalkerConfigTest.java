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
    void aDefaultWalkPlansCanoesAndMinigameTeleports() {
        GlobalPathfinderConfig config = WalkerConfig.builder().build().getPathfinderConfig();

        assertTrue(config.isUseCanoes());
        assertTrue(config.isUseTeleportationMinigames());
    }

    @Test
    void aWalkCanLeaveThoseKindsOut() {
        GlobalPathfinderConfig config = WalkerConfig.builder()
                .pathfinderConfig(GlobalPathfinderConfig.builder()
                        .useCanoes(false)
                        .useTeleportationMinigames(false)
                        .build())
                .build()
                .getPathfinderConfig();

        assertFalse(config.isUseCanoes());
        assertFalse(config.isUseTeleportationMinigames());
    }
}
