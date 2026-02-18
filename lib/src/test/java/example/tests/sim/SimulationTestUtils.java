package example.tests.sim;

import com.kraken.api.sim.colosim.Simulation;
import com.kraken.api.sim.colosim.model.Mob;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class SimulationTestUtils {
    private SimulationTestUtils() {
    }

    public static Mob mobAt(Simulation simulation, int idx) {
        return simulation.getMobs().get(idx);
    }

    public static void assertMobPosition(Mob mob, int x, int y) {
        assertEquals(x, mob.getX(), "x");
        assertEquals(y, mob.getY(), "y");
    }

    public static int[] lineAtTick(Simulation simulation, int tickOneBased) {
        return simulation.getTape().get(tickOneBased - 1);
    }

    public static boolean attackedAt(Simulation simulation, int tickOneBased, int mobIndex) {
        return (lineAtTick(simulation, tickOneBased)[mobIndex] & 0xff) != 0;
    }
}
