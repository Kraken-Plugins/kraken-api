package example.tests.sim;

import com.kraken.api.sim.colosim.Mob;
import com.kraken.api.sim.colosim.NpcType;
import com.kraken.api.sim.colosim.Simulation;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static example.tests.sim.SimulationTestUtils.mobAt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ManticoreBehaviorTest {
    @Test
    void twoUnknownManticoresSyncToSameStyleWhenChargingTogether() {
        Simulation simulation = new Simulation();
        simulation.setPlayer(16, 18);
        simulation.placeMob(3, 19, NpcType.MANTICORE, "u");
        simulation.placeMob(9, 17, NpcType.MANTICORE, "u");

        simulation.step();
        Mob first = mobAt(simulation, 0);
        Mob second = mobAt(simulation, 1);

        assertTrue(first.cooldown > 0);
        assertTrue(second.cooldown > 0);
        assertEquals(first.extra, second.extra);
        assertTrue(Set.of("r", "m").contains(first.extra));
    }

    @Test
    void unknownDefersToUmOrUrWhenChargingSameTick() {
        Simulation simulation = new Simulation();
        simulation.setPlayer(16, 18);
        simulation.placeMob(3, 19, NpcType.MANTICORE, "u");
        simulation.placeMob(9, 17, NpcType.MANTICORE, "um");

        simulation.step();
        Mob unknown = mobAt(simulation, 0);
        Mob known = mobAt(simulation, 1);
        assertEquals("m", unknown.extra);
        assertEquals("m", known.extra);

        simulation.clear();
        simulation.setPlayer(16, 18);
        simulation.placeMob(3, 19, NpcType.MANTICORE, "u");
        simulation.placeMob(9, 17, NpcType.MANTICORE, "ur");
        simulation.step();

        unknown = mobAt(simulation, 0);
        known = mobAt(simulation, 1);
        assertEquals("r", unknown.extra);
        assertEquals("r", known.extra);
    }

    @Test
    void resetRestoresOriginalExtraValues() {
        Simulation simulation = new Simulation();
        simulation.setPlayer(16, 18);
        simulation.placeMob(10, 19, NpcType.MANTICORE, "ur");
        simulation.placeMob(20, 15, NpcType.MANTICORE, "u");

        simulation.step();
        Mob urManti = mobAt(simulation, 0);
        Mob uManti = mobAt(simulation, 1);
        assertEquals("r", urManti.extra);
        assertEquals("r", uManti.extra);
        assertEquals("ur", urManti.originalExtra);
        assertEquals("u", uManti.originalExtra);

        simulation.reset();
        assertEquals("ur", mobAt(simulation, 0).extra);
        assertEquals("u", mobAt(simulation, 1).extra);
    }

    @Test
    void loneUnknownManticoreBecomesPermanentUrOrUmAfterRandomChoice() {
        Simulation simulation = new Simulation();
        simulation.setPlayer(16, 18);
        simulation.placeMob(20, 15, NpcType.MANTICORE, "u");

        simulation.step();
        Mob manti = mobAt(simulation, 0);
        assertTrue(Set.of("r", "m").contains(manti.extra));

        String expectedOriginal = "r".equals(manti.extra) ? "ur" : "um";
        assertEquals(expectedOriginal, manti.originalExtra);

        simulation.reset();
        assertEquals(expectedOriginal, mobAt(simulation, 0).extra);
    }

    @Test
    void onlyOneManticoreFiresPerTickAndOthersGetDelayed() {
        Simulation simulation = new Simulation();
        simulation.setPlayer(16, 18);
        simulation.placeMob(9, 17, NpcType.MANTICORE, "r");
        simulation.placeMob(10, 17, NpcType.MANTICORE, "r");

        simulation.step();
        int[] line = simulation.getTape().get(simulation.getTape().size() - 1);
        int attackedCount = 0;
        for (int value : line) {
            if ((value & 0xff) != 0) {
                attackedCount++;
            }
        }
        assertEquals(1, attackedCount);
        assertEquals(5, mobAt(simulation, 1).cooldown);
    }
}
