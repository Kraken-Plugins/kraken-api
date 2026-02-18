package example.tests.sim;

import com.kraken.api.sim.colosim.NpcType;
import com.kraken.api.sim.colosim.Simulation;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static example.tests.sim.SimulationTestUtils.attackedAt;
import static example.tests.sim.SimulationTestUtils.lineAtTick;
import static example.tests.sim.SimulationTestUtils.mobAt;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PillarStackBehaviorTest {
    @Test
    void stackedShamansByPillarHaveDeterministicLosAndAttackTicks() {
        Simulation simulation = new Simulation();
        simulation.setPlayer(7, 9);
        simulation.placeMob(11, 7, NpcType.SERPENT_SHAMAN, null);
        simulation.placeMob(11, 8, NpcType.SERPENT_SHAMAN, null);
        simulation.placeMob(11, 9, NpcType.SERPENT_SHAMAN, null);

        List<boolean[]> losByTick = new ArrayList<>();
        for (int tick = 1; tick <= 9; tick++) {
            if (tick == 5) {
                simulation.setPlayer(7, 7);
            }
            if (tick == 9) {
                simulation.setPlayer(7, 9);
            }
            simulation.step();
            losByTick.add(losForAllMobs(simulation));
        }

        assertEquals(List.of(4, 9), attackTicksForMob(simulation, 0));
        assertEquals(List.of(5), attackTicksForMob(simulation, 1));
        assertEquals(List.of(), attackTicksForMob(simulation, 2));

        assertEquals(10, xAt(simulation, 1, 0));
        assertEquals(9, xAt(simulation, 2, 0));
        assertEquals(8, xAt(simulation, 3, 0));
        assertEquals(7, xAt(simulation, 4, 0));
        assertEquals(7, yAt(simulation, 4, 0));
        assertEquals(7, yAt(simulation, 5, 1));
        assertEquals(8, yAt(simulation, 5, 2));

        boolean[][] expectedLos = {
                {false, false, false},
                {false, false, false},
                {false, false, false},
                {true, false, false},
                {false, true, false},
                {false, true, false},
                {false, true, false},
                {false, true, false},
                {true, false, false}
        };
        for (int tick = 1; tick <= expectedLos.length; tick++) {
            assertArrayEquals(expectedLos[tick - 1], losByTick.get(tick - 1), "tick " + tick);
        }
    }

    @Test
    void mixedNpcStackAcrossPillarTracksLosAndAttackAsPlayerWiggles() {
        Simulation simulation = new Simulation();
        simulation.setPlayer(7, 9);
        simulation.placeMob(11, 8, NpcType.SERPENT_SHAMAN, null);
        simulation.placeMob(11, 12, NpcType.JAVELIN_COLOSSUS, null);
        simulation.placeMob(11, 16, NpcType.SHOCKWAVE_COLOSSUS, null);

        List<boolean[]> losByTick = new ArrayList<>();
        for (int tick = 1; tick <= 16; tick++) {
            if (tick == 4) {
                simulation.setPlayer(7, 7);
            }
            if (tick == 8) {
                simulation.setPlayer(7, 9);
            }
            if (tick == 11) {
                simulation.setPlayer(7, 12);
            }
            if (tick == 14) {
                simulation.setPlayer(7, 9);
            }
            simulation.step();
            losByTick.add(losForAllMobs(simulation));
        }

        assertEquals(List.of(5, 11, 16), attackTicksForMob(simulation, 0));
        assertEquals(List.of(12), attackTicksForMob(simulation, 1));
        assertEquals(List.of(3, 8, 14), attackTicksForMob(simulation, 2));

        assertEquals(11, xAt(simulation, 1, 1));
        assertEquals(12, yAt(simulation, 1, 1));
        assertEquals(11, xAt(simulation, 5, 1));
        assertEquals(10, yAt(simulation, 5, 1));
        assertEquals(8, xAt(simulation, 3, 2));
        assertEquals(14, yAt(simulation, 3, 2));
        assertEquals(7, xAt(simulation, 4, 2));
        assertEquals(13, yAt(simulation, 4, 2));

        boolean[][] expectedLos = {
                {false, false, false},
                {false, false, false},
                {false, false, true},
                {false, false, true},
                {true, false, true},
                {true, false, true},
                {true, false, true},
                {false, false, true},
                {false, false, true},
                {false, false, true},
                {true, false, false},
                {true, true, false},
                {true, true, false},
                {true, false, true},
                {true, false, true},
                {true, false, true}
        };
        for (int tick = 1; tick <= expectedLos.length; tick++) {
            assertArrayEquals(expectedLos[tick - 1], losByTick.get(tick - 1), "tick " + tick);
        }
    }

    private static List<Integer> attackTicksForMob(Simulation simulation, int mobIndex) {
        List<Integer> ticks = new ArrayList<>();
        for (int tick = 1; tick <= simulation.getTape().size(); tick++) {
            if (attackedAt(simulation, tick, mobIndex)) {
                ticks.add(tick);
            }
        }
        return ticks;
    }

    private static boolean[] losForAllMobs(Simulation simulation) {
        boolean[] los = new boolean[simulation.getMobs().size()];
        for (int i = 0; i < simulation.getMobs().size(); i++) {
            los[i] = simulation.canAttackPlayer(mobAt(simulation, i));
        }
        return los;
    }

    private static int xAt(Simulation simulation, int tick, int mobIndex) {
        return (lineAtTick(simulation, tick)[mobIndex] >> 16) & 0xff;
    }

    private static int yAt(Simulation simulation, int tick, int mobIndex) {
        return (lineAtTick(simulation, tick)[mobIndex] >> 24) & 0xff;
    }
}
