package example.tests.sim;

import com.kraken.api.sim.colosim.Mob;
import com.kraken.api.sim.colosim.NpcType;
import com.kraken.api.sim.colosim.Simulation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class PlacementAndSortingTest {
    @Test
    void placeDisallowPlayer() {
        Simulation simulation = new Simulation();
        boolean placed = simulation.placeMob(1, 1, NpcType.PLAYER, null);
        assertFalse(placed);
        assertTrue(simulation.getMobs().isEmpty());
    }

    @Test
    void placeSingleNpcAndNoOverlapSpawn() {
        Simulation simulation = new Simulation();
        assertTrue(simulation.placeMob(1, 1, NpcType.SERPENT_SHAMAN, null));
        assertFalse(simulation.placeMob(1, 1, NpcType.JAVELIN_COLOSSUS, null));

        assertEquals(1, simulation.getMobs().size());
        Mob mob = simulation.getMobs().get(0);
        assertEquals(1, mob.x);
        assertEquals(1, mob.y);
        assertEquals(NpcType.SERPENT_SHAMAN.typeId, mob.type);
        assertEquals(0, mob.cooldown);
    }

    @Test
    void npcsSortedByNpcIdBehavior() {
        Simulation simulation = new Simulation();
        simulation.placeMob(0, 0, NpcType.SERPENT_SHAMAN, null);
        simulation.placeMob(0, 1, NpcType.SHOCKWAVE_COLOSSUS, null);
        simulation.placeMob(0, 2, NpcType.MINOTAUR, null);
        simulation.placeMob(0, 3, NpcType.JAVELIN_COLOSSUS, null);
        simulation.placeMob(0, 4, NpcType.MANTICORE, "u");

        List<Integer> types = simulation.getMobs().stream().map(m -> m.type).collect(Collectors.toList());
        assertEquals(List.of(
                NpcType.MANTICORE.typeId,
                NpcType.SERPENT_SHAMAN.typeId,
                NpcType.JAVELIN_COLOSSUS.typeId,
                NpcType.SHOCKWAVE_COLOSSUS.typeId,
                NpcType.MINOTAUR.typeId
        ), types);
    }
}
