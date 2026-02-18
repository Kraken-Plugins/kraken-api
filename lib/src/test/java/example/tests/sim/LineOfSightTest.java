package example.tests.sim;

import com.kraken.api.sim.colosim.Simulation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LineOfSightTest {
    @Test
    void sameTileOrCollisionIsNotLos() {
        Simulation simulation = new Simulation();
        assertFalse(simulation.hasLOS(10, 10, 10, 10, 1, 10, false));
        assertFalse(simulation.hasLOS(10, 10, 10, 9, 2, 10, false));
    }

    @Test
    void rangedLosBlockedByOuterWallTiles() {
        Simulation simulation = new Simulation();
        assertFalse(simulation.hasLOS(10, 10, 0, 10, 1, 15, false));
    }

    @Test
    void meleeGeometryMatchesPathingExpectations() {
        Simulation simulation = new Simulation();
        assertTrue(simulation.hasLOS(11, 9, 11, 7, 2, 1, true));
        assertFalse(simulation.hasLOS(11, 9, 10, 7, 2, 1, true));
    }

    @Test
    void npcLosUsesClosestHitTile() {
        Simulation simulation = new Simulation();
        assertTrue(simulation.hasLOS(12, 12, 16, 16, 3, 15, true));
    }

    @Test
    void clearOpenRangeLineExists() {
        Simulation simulation = new Simulation();
        assertTrue(simulation.hasLOS(16, 16, 20, 16, 1, 15, false));
    }
}
