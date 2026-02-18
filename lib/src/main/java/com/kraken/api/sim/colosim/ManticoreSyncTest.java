package com.kraken.api.sim.colosim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ManticoreSyncTest {

    @BeforeEach
    public void setUp() {
        LineOfSight.remove();
    }

    @Test
    public void twoUnknownManticoresGainingLosSimultaneouslyShouldChargeWithSameOrbOrder() {
        // Place player at position
        LineOfSight._setSelected(new Types.Coordinates(16, 18), 0, null);

        // Place first unknown manticore 'u'
        LineOfSight._setSelected(new Types.Coordinates(3, 19), LineOfSight.MANTICORE, "u");
        LineOfSight.place();

        // Place second unknown manticore 'u'
        LineOfSight._setSelected(new Types.Coordinates(9, 17), LineOfSight.MANTICORE, "u");
        LineOfSight.place();

        // Move player to trigger los
        LineOfSight._setSelected(new Types.Coordinates(16, 18), 0, null);

        // Step forward to trigger charging
        LineOfSight.step(false);

        java.util.List<Types.Mob> mobs = LineOfSight._getMobs();
        Types.Mob firstManticore = mobs.get(0);
        Types.Mob secondManticore = mobs.get(1);

        // Both should be charging
        assertTrue(firstManticore.cooldown > 0);
        assertTrue(secondManticore.cooldown > 0);

        // Both should have the SAME style (either both 'r' or both 'm')
        assertEquals(firstManticore.extra, secondManticore.extra);
        assertTrue(firstManticore.extra.equals("r") || firstManticore.extra.equals("m"));
    }
}
