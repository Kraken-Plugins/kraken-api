package com.kraken.api.sim.colosim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class NpcIdWiggleTest {

    @BeforeEach
    public void setUp() {
        LineOfSight.remove();
    }

    @Test
    public void checkNpcsAreSortedByNpcId() {
        LineOfSight._setSelected(new Types.Coordinates(0, 0), LineOfSight.NpcType.SERPENT_SHAMAN, null);
        LineOfSight.place();
        LineOfSight._setSelected(new Types.Coordinates(0, 1), LineOfSight.NpcType.SHOCKWAVE_COLOSSUS, null);
        LineOfSight.place();
        LineOfSight._setSelected(new Types.Coordinates(0, 2), LineOfSight.NpcType.MINOTAUR, null);
        LineOfSight.place();
        LineOfSight._setSelected(new Types.Coordinates(0, 3), LineOfSight.NpcType.JAVELIN_COLOSSUS, null);
        LineOfSight.place();
        LineOfSight._setSelected(new Types.Coordinates(0, 4), LineOfSight.NpcType.MANTICORE, null);
        LineOfSight.place();

        java.util.List<Types.Mob> mobs = LineOfSight._getMobs();
        int[] expectedTypes = {
                LineOfSight.NpcType.MANTICORE,
                LineOfSight.NpcType.SERPENT_SHAMAN,
                LineOfSight.NpcType.JAVELIN_COLOSSUS,
                LineOfSight.NpcType.SHOCKWAVE_COLOSSUS,
                LineOfSight.NpcType.MINOTAUR
        };

        for (int i = 0; i < mobs.size(); i++) {
            assertEquals(expectedTypes[i], mobs.get(i).type);
        }
    }

    @Test
    public void checkManticoreMovesBeforeShaman() {
        LineOfSight._setSelected(new Types.Coordinates(11, 9), LineOfSight.NpcType.SERPENT_SHAMAN, null);
        LineOfSight.place();
        LineOfSight._setSelected(new Types.Coordinates(12, 9), LineOfSight.NpcType.MANTICORE, null);
        LineOfSight.place();

        // manti wiggles north
        LineOfSight._setSelected(new Types.Coordinates(7, 8), LineOfSight.NpcType.PLAYER, null);
        LineOfSight.step(false);

        java.util.List<Types.Mob> mobs = LineOfSight._getMobs();
        Types.Mob manticore = mobs.get(0);
        Types.Mob shaman = mobs.get(1);

        assertEquals(11, manticore.x);
        assertEquals(8, manticore.y);
        assertEquals(11, shaman.x);
        assertEquals(9, shaman.y);
    }

    // ... Add other test cases similarly ...
}
