package example.tests.sim;

import com.kraken.api.simulation.colosim.NpcType;
import com.kraken.api.simulation.colosim.Simulation;
import com.kraken.api.simulation.colosim.model.Mob;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static example.tests.sim.SimulationTestUtils.assertMobPosition;
import static example.tests.sim.SimulationTestUtils.mobAt;

class FromWaveStartTest {

    @Test
    void firstTickNoMovementSecondTickMoves() {
        Simulation simulation = new Simulation();
        simulation.setFromWaveStart(true);
        simulation.placeMob(20, 13, NpcType.JAVELIN_COLOSSUS, null);
        simulation.setPlayer(7, 8);

        simulation.step();
        assertMobPosition(mobAt(simulation, 0), 20, 13);

        simulation.step();
        assertMobPosition(mobAt(simulation, 0), 19, 12);
    }

    @Test
    void attacksDelayedUntilFourthTick() {
        Simulation simulation = new Simulation();
        simulation.setFromWaveStart(true);
        simulation.placeMob(12, 12, NpcType.JAVELIN_COLOSSUS, null);
        simulation.setPlayer(16, 16);

        simulation.step();
        Mob javelin = mobAt(simulation, 0);
        Assertions.assertEquals(-1, javelin.getCooldown());

        simulation.step();
        assertMobPosition(javelin, 13, 13);
        Assertions.assertEquals(-2, javelin.getCooldown());

        simulation.step();
        Assertions.assertEquals(-3, javelin.getCooldown());

        simulation.step();
        Assertions.assertEquals(5, javelin.getCooldown());
    }
}
