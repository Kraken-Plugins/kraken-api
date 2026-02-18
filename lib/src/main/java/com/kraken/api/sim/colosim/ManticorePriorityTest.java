package com.kraken.api.sim.colosim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ManticorePriorityTest {

    @BeforeEach
    public void setUp() {
        LineOfSight.remove();
    }

    @Test
    public void shouldUseUmManticoreToDetermineOrbOrderWhenBothUmAndUSeePlayer() {
        // Place player at position
        LineOfSight._setSelected(new Types.Coordinates(16, 18), 0, null);

        // Place an unknown manticore 'u'
        LineOfSight._setSelected(new Types.Coordinates(3, 19), LineOfSight.MANTICORE, "u");
        LineOfSight.place();

        // Place an uncharged mage manticore 'um'
        LineOfSight._setSelected(new Types.Coordinates(9, 17), LineOfSight.MANTICORE, "um");
        LineOfSight.place();

        // Move player to trigger los
        LineOfSight._setSelected(new Types.Coordinates(16, 18), 0, null);

        // Step forward to trigger charging
        LineOfSight.step(false);

        java.util.List<Types.Mob> mobs = LineOfSight._getMobs();
        Types.Mob unknownManticore = mobs.get(0);
        Types.Mob umManticore = mobs.get(1);

        // Both should be charging
        assertTrue(unknownManticore.cooldown > 0);
        assertTrue(umManticore.cooldown > 0);

        // Both should have mage style since 'um' has priority
        assertEquals("m", unknownManticore.extra);
        assertEquals("m", umManticore.extra);
    }

    @Test
    public void shouldUseUrManticoreToDetermineOrbOrderWhenBothUrAndUSeePlayer() {
        // Place player at position
        LineOfSight._setSelected(new Types.Coordinates(16, 18), 0, null);

        // Place an unknown manticore 'u'
        LineOfSight._setSelected(new Types.Coordinates(3, 19), LineOfSight.MANTICORE, "u");
        LineOfSight.place();

        // Place an uncharged range manticore 'ur'
        LineOfSight._setSelected(new Types.Coordinates(9, 17), LineOfSight.MANTICORE, "ur");
        LineOfSight.place();

        // Move player to trigger los
        LineOfSight._setSelected(new Types.Coordinates(16, 18), 0, null);

        // Step forward to trigger charging
        LineOfSight.step(false);

        java.util.List<Types.Mob> mobs = LineOfSight._getMobs();
        Types.Mob unknownManticore = mobs.get(0);
        Types.Mob urManticore = mobs.get(1);

        // Both should be charging
        assertTrue(unknownManticore.cooldown > 0);
        assertTrue(urManticore.cooldown > 0);

        // Both should have range style since 'ur' has priority
        assertEquals("r", unknownManticore.extra);
        assertEquals("r", urManticore.extra);
    }

    @Test
    public void shouldInheritFromChargedManticoreRWhenBothSeePlayer() {
        // Place player at position
        LineOfSight._setSelected(new Types.Coordinates(16, 18), 0, null);

        // Place a charged range manticore
        LineOfSight._setSelected(new Types.Coordinates(3, 19), LineOfSight.MANTICORE, "r");
        LineOfSight.place();

        // Place an uncharged mage manticore 'um'
        LineOfSight._setSelected(new Types.Coordinates(9, 17), LineOfSight.MANTICORE, "um");
        LineOfSight.place();

        // Move player to trigger los
        LineOfSight._setSelected(new Types.Coordinates(16, 18), 0, null);

        // Step forward to trigger charging
        LineOfSight.step(false);

        java.util.List<Types.Mob> mobs = LineOfSight._getMobs();
        Types.Mob chargedManticore = mobs.get(0);
        Types.Mob umManticore = mobs.get(1);

        // UM should inherit range style from charged manticore
        assertTrue(umManticore.cooldown > 0);
        assertEquals("r", umManticore.extra);

        // Charged manticore keeps its style
        assertEquals("r", chargedManticore.extra);

        // UM should keep its original type
        assertEquals("um", umManticore.originalExtra);
    }

    // ... Add other test cases similarly ...
}
