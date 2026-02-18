package example.tests.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class SimulationTestUtils {
    private SimulationTestUtils() {
    }

    public static Mob mobAt(Simulation simulation, int idx) {
        return simulation.getMobs().get(idx);
    }

    public static void assertMobPosition(Mob mob, int x, int y) {
        assertEquals(x, mob.x, "x");
        assertEquals(y, mob.y, "y");
    }
}
