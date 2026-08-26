package unit.com.kraken.api.service.walker.transport;

import com.kraken.api.service.walker.transport.DisplayInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers reading a hub transport's destination out of its display info.
 *
 * <p>Every string here comes from the expanded dataset. The traps are the entries that look like a
 * numbered menu but are not — a teleport item's "Ardougne cloak: Kandarin Monastery" must keep its
 * whole name — and the fairy ring chain that no single selection can express.</p>
 */
class DisplayInfoTest {

    @Test
    void nullAndBlankYieldNothing() {
        assertNull(DisplayInfo.parse(null));
        assertNull(DisplayInfo.parse("   "));
    }

    @Test
    void aBareNameIsTheLabel() {
        DisplayInfo info = DisplayInfo.parse("Aldarin");

        assertEquals("Aldarin", info.getLabel());
        assertFalse(info.hasPosition());
        assertFalse(info.isFairyRing());
    }

    @Test
    void aNumberedMenuEntrySplits() {
        DisplayInfo info = DisplayInfo.parse("6: Prifddinas");

        assertEquals("Prifddinas", info.getLabel());
        assertEquals(6, info.getPosition());
        assertTrue(info.hasPosition());
    }

    @Test
    void aFullStopSeparatorAlsoSplits() {
        DisplayInfo info = DisplayInfo.parse("4. Mushroom Meadow");

        assertEquals("Mushroom Meadow", info.getLabel());
        assertEquals(4, info.getPosition());
    }

    @Test
    void aLetteredEntryContinuesPastNine() {
        DisplayInfo info = DisplayInfo.parse("B: Farming Guild");

        assertEquals("Farming Guild", info.getLabel());
        assertEquals(11, info.getPosition());
    }

    @Test
    void anObeliskEntryKeepsItsWholeName() {
        DisplayInfo info = DisplayInfo.parse("1: Level 13 Wilderness");

        assertEquals("Level 13 Wilderness", info.getLabel());
        assertEquals(1, info.getPosition());
    }

    @Test
    void aFairyRingCodeIsRecognised() {
        DisplayInfo info = DisplayInfo.parse("A L Q");

        assertTrue(info.isFairyRing());
        assertEquals("ALQ", info.getFairyRingCode());
    }

    @Test
    void everyDialLetterIsAccepted() {
        assertEquals("DLS", DisplayInfo.parse("D L S").getFairyRingCode());
        assertEquals("AIP", DisplayInfo.parse("A I P").getFairyRingCode());
    }

    @Test
    void aChainOfCodesIsNotASingleRing() {
        DisplayInfo info = DisplayInfo.parse("A I R - D L R - D J Q - A J S");

        assertFalse(info.isFairyRing());
        assertNull(info.getFairyRingCode());
    }

    @Test
    void lettersOutsideTheDialsAreNotACode() {
        assertFalse(DisplayInfo.parse("X Y Z").isFairyRing());
        assertFalse(DisplayInfo.parse("A A A").isFairyRing());
    }

    @Test
    void aTeleportItemKeepsItsItemNameOutOfThePosition() {
        DisplayInfo info = DisplayInfo.parse("Ardougne cloak: Kandarin Monastery");

        assertFalse(info.hasPosition());
        assertEquals("Ardougne cloak: Kandarin Monastery", info.getLabel());
    }

    @Test
    void aMinigameTeleportIsABareName() {
        DisplayInfo info = DisplayInfo.parse("Barbarian Assault Minigame Teleport");

        assertEquals("Barbarian Assault Minigame Teleport", info.getLabel());
        assertFalse(info.hasPosition());
    }

    @Test
    void aMinecartEntrySplits() {
        DisplayInfo info = DisplayInfo.parse("1: Arceuus");

        assertEquals("Arceuus", info.getLabel());
        assertEquals(1, info.getPosition());
    }
}
