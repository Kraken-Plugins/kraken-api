package com.kraken.api.sim.colosim;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ManticoreResetBehaviorTest {

    @BeforeEach
    public void setUp() {
        LineOfSight.remove();
        LineOfSight.setFromWaveStart(false);
    }

    @Test
    public void uManticoreInheritingFromUrManticoreShouldResetBackToU() {
        // Place player at position where both manticores will have LOS
        LineOfSight._setSelected(new Types.Coordinates(16, 18), 0, null);

        // Place an uncharged-but-known 'ur' manticore (within 15 range)
        LineOfSight._setSelected(new Types.Coordinates(10, 19), LineOfSight.MANTICORE, "ur");
        LineOfSight.place();

        // Place an unknown 'u' manticore (within 15 range)
        LineOfSight._setSelected(new Types.Coordinates(20, 15), LineOfSight.MANTICORE, "u");
        LineOfSight.place();

        // Reset player position after placing NPCs
        LineOfSight._setSelected(new Types.Coordinates(16, 18), 0, null);

        // Step to trigger charging
        LineOfSight.step(false);

        java.util.List<Types.Mob> mobs = LineOfSight._getMobs();
        Types.Mob urManticore = mobs.get(0);
        Types.Mob uManticore = mobs.get(1);

        // Both should have 'r' style (u inherited from ur)
        assertEquals("r", urManticore.extra);
        assertEquals("r", uManticore.extra);

        // ur should keep originalExtra as 'ur', u should keep originalExtra as 'u'
        assertEquals("ur", urManticore.originalExtra);
        assertEquals("u", uManticore.originalExtra);

        // Reset
        LineOfSight.reset();

        mobs = LineOfSight._getMobs();
        Types.Mob urManticoreAfterReset = mobs.get(0);
        Types.Mob uManticoreAfterReset = mobs.get(1);

        // ur manticore should reset to uncharged with extra='ur'
        assertEquals("ur", urManticoreAfterReset.extra);
        assertEquals("ur", urManticoreAfterReset.originalExtra);

        // u manticore should reset back to unknown
        assertEquals("u", uManticoreAfterReset.extra);
        assertEquals("u", uManticoreAfterReset.originalExtra);
    }

    @Test
    public void uManticoreInheritingFromChargedRManticoreShouldResetBackToU() {
        // Place player at position where both manticores will have LOS
        LineOfSight._setSelected(new Types.Coordinates(16, 18), 0, null);

        // Place a charged 'r' manticore (within 15 range)
        LineOfSight._setSelected(new Types.Coordinates(10, 19), LineOfSight.MANTICORE, "r");
        LineOfSight.place();

        // Place an unknown 'u' manticore (within 15 range)
        LineOfSight._setSelected(new Types.Coordinates(20, 15), LineOfSight.MANTICORE, "u");
        LineOfSight.place();

        // Reset player position after placing NPCs
        LineOfSight._setSelected(new Types.Coordinates(16, 18), 0, null);

        // Step to trigger charging (u will inherit from charged r)
        LineOfSight.step(false);

        java.util.List<Types.Mob> mobs = LineOfSight._getMobs();
        Types.Mob rManticore = mobs.get(0);
        Types.Mob uManticore = mobs.get(1);

        // Both should have 'r' style
        assertEquals("r", rManticore.extra);
        assertEquals("r", uManticore.extra);

        // r should keep originalExtra as 'r', u should keep originalExtra as 'u'
        assertEquals("r", rManticore.originalExtra);
        assertEquals("u", uManticore.originalExtra);

        // Reset
        LineOfSight.reset();

        mobs = LineOfSight._getMobs();
        Types.Mob rManticoreAfterReset = mobs.get(0);
        Types.Mob uManticoreAfterReset = mobs.get(1);

        // r manticore should reset to charged with extra='r'
        assertEquals("r", rManticoreAfterReset.extra);
        assertEquals("r", rManticoreAfterReset.originalExtra);

        // u manticore should reset back to unknown
        assertEquals("u", uManticoreAfterReset.extra);
        assertEquals("u", uManticoreAfterReset.originalExtra);
    }

    @Test
    public void uManticoreChoosingRandomlyShouldBecomeUrOrUmPermanently() {
        // Place player at position where manticore will have LOS
        LineOfSight._setSelected(new Types.Coordinates(16, 18), 0, null);

        // Place only an unknown 'u' manticore (no others to inherit from)
        LineOfSight._setSelected(new Types.Coordinates(20, 15), LineOfSight.MANTICORE, "u");
        LineOfSight.place();

        // Reset player position after placing NPC
        LineOfSight._setSelected(new Types.Coordinates(16, 18), 0, null);

        // Step to trigger charging
        LineOfSight.step(false);

        java.util.List<Types.Mob> mobs = LineOfSight._getMobs();
        Types.Mob manticore = mobs.get(0);

        // Should have chosen 'r' or 'm' randomly
        assertTrue(manticore.extra.equals("r") || manticore.extra.equals("m"));

        // originalExtra should have been updated to 'ur' or 'um'
        String expectedOriginal = manticore.extra.equals("r") ? "ur" : "um";
        assertEquals(expectedOriginal, manticore.originalExtra);

        // Reset
        LineOfSight.reset();

        mobs = LineOfSight._getMobs();
        Types.Mob manticoreAfterReset = mobs.get(0);

        // Should maintain the chosen style (now as uncharged version)
        assertEquals(expectedOriginal, manticoreAfterReset.extra);
        assertEquals(expectedOriginal, manticoreAfterReset.originalExtra);
    }
}
