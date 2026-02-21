package unit.com.kraken.api.simulation.colosim;

import com.kraken.api.simulation.colosim.NpcType;
import com.kraken.api.simulation.colosim.Simulation;
import com.kraken.api.simulation.colosim.model.Mob;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static unit.com.kraken.api.simulation.colosim.SimulationTestUtils.mobAt;
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

        assertTrue(first.getCooldown() > 0);
        assertTrue(second.getCooldown() > 0);
        assertEquals(first.getExtra(), second.getExtra());
        assertTrue(Set.of("r", "m").contains(first.getExtra()));
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
        assertEquals("m", unknown.getExtra());
        assertEquals("m", known.getExtra());

        simulation.clear();
        simulation.setPlayer(16, 18);
        simulation.placeMob(3, 19, NpcType.MANTICORE, "u");
        simulation.placeMob(9, 17, NpcType.MANTICORE, "ur");
        simulation.step();

        unknown = mobAt(simulation, 0);
        known = mobAt(simulation, 1);
        assertEquals("r", unknown.getExtra());
        assertEquals("r", known.getExtra());
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
        assertEquals("r", urManti.getExtra());
        assertEquals("r", uManti.getExtra());
        assertEquals("ur", urManti.getOriginalExtra());
        assertEquals("u", uManti.getOriginalExtra());

        simulation.reset();
        assertEquals("ur", mobAt(simulation, 0).getExtra());
        assertEquals("u", mobAt(simulation, 1).getExtra());
    }

    @Test
    void loneUnknownManticoreBecomesPermanentUrOrUmAfterRandomChoice() {
        Simulation simulation = new Simulation();
        simulation.setPlayer(16, 18);
        simulation.placeMob(20, 15, NpcType.MANTICORE, "u");

        simulation.step();
        Mob manti = mobAt(simulation, 0);
        assertTrue(Set.of("r", "m").contains(manti.getExtra()));

        String expectedOriginal = "r".equals(manti.getExtra()) ? "ur" : "um";
        assertEquals(expectedOriginal, manti.getOriginalExtra());

        simulation.reset();
        assertEquals(expectedOriginal, mobAt(simulation, 0).getExtra());
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
        assertEquals(5, mobAt(simulation, 1).getCooldown());
    }
}
