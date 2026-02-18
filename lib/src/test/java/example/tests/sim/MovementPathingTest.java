package example.tests.sim;

import org.junit.jupiter.api.Test;

import static colosim.SimulationTestUtils.assertMobPosition;
import static colosim.SimulationTestUtils.mobAt;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MovementPathingTest {
    @Test
    void singleTileNpcCannotDiagonalToPathOrAttack() {
        Simulation simulation = new Simulation();
        simulation.placeMob(8, 7, NpcType.SERPENT_SHAMAN, null);

        simulation.setPlayer(7, 20);
        simulation.step();
        assertMobPosition(mobAt(simulation, 0), 7, 7);
        simulation.step();
        assertMobPosition(mobAt(simulation, 0), 7, 8);

        simulation.setPlayer(10, 7);
        simulation.step();
        Mob shaman = mobAt(simulation, 0);
        assertMobPosition(shaman, 7, 8);
        assertEquals(5, shaman.cooldown);
    }

    @Test
    void trappedMeleeMatchesExpectedMovementPattern() {
        Simulation simulation = new Simulation();
        simulation.placeMob(8, 13, NpcType.JAGUAR_WARRIOR, null);

        simulation.setPlayer(7, 8);
        simulation.step();
        assertMobPosition(mobAt(simulation, 0), 7, 12);

        simulation.step();
        assertMobPosition(mobAt(simulation, 0), 7, 12);

        simulation.setPlayer(11, 7);
        simulation.step();
        assertMobPosition(mobAt(simulation, 0), 8, 12);
        simulation.step();
        assertMobPosition(mobAt(simulation, 0), 9, 12);
        simulation.step();
        assertMobPosition(mobAt(simulation, 0), 10, 12);
        simulation.step();
        assertMobPosition(mobAt(simulation, 0), 11, 11);
        simulation.step();
        assertMobPosition(mobAt(simulation, 0), 11, 10);
        simulation.step();

        Mob meleer = mobAt(simulation, 0);
        assertMobPosition(meleer, 11, 9);
        assertEquals(5, meleer.cooldown);

        simulation.setPlayer(10, 7);
        simulation.step();
        assertMobPosition(meleer, 11, 9);

        simulation.setPlayer(10, 6);
        simulation.step();
        assertMobPosition(meleer, 11, 8);
    }

    @Test
    void wiggleOrderingManticoreMovesBeforeShaman() {
        Simulation simulation = new Simulation();
        simulation.placeMob(11, 9, NpcType.SERPENT_SHAMAN, null);
        simulation.placeMob(12, 9, NpcType.MANTICORE, "u");

        simulation.setPlayer(7, 8);
        simulation.step();

        Mob manticore = mobAt(simulation, 0);
        Mob shaman = mobAt(simulation, 1);
        assertMobPosition(manticore, 11, 8);
        assertMobPosition(shaman, 11, 9);
    }
}
