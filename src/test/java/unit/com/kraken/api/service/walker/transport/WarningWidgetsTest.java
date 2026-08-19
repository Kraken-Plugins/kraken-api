package unit.com.kraken.api.service.walker.transport;

import com.kraken.api.service.walker.transport.WarningWidgets;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers deciding what counts as a transport warning overlay.
 *
 * <p>The spellbook always offers a {@code Warnings} filter. Matching that as a warning title is what
 * cancelled staircase climbs: the walker clicked the magic tab instead of waiting for the climb. The
 * wilderness ditch uses a longer title, so overlay matching has to accept that without accepting the
 * filter.</p>
 */
class WarningWidgetsTest {

    @Test
    void aBareWarningTitleCounts() {
        assertTrue(WarningWidgets.isWarningTitle("WARNING"));
        assertTrue(WarningWidgets.isWarningTitle("Warning"));
        assertTrue(WarningWidgets.isWarningTitle("warning"));
        assertTrue(WarningWidgets.isOverlayTitle("WARNING"));
    }

    @Test
    void colourTagsAndTrailingPunctuationAreIgnored() {
        assertTrue(WarningWidgets.isWarningTitle("<col=ff0000>WARNING</col>"));
        assertTrue(WarningWidgets.isWarningTitle("WARNING!"));
        assertTrue(WarningWidgets.isWarningTitle("  WARNING  "));
        assertTrue(WarningWidgets.isOverlayTitle("<col=ff0000>WARNING</col>"));
    }

    @Test
    void theSpellbookWarningsFilterDoesNotCount() {
        assertFalse(WarningWidgets.isWarningTitle("Warnings"));
        assertFalse(WarningWidgets.isOverlayTitle("Warnings"));
        assertFalse(WarningWidgets.isOverlayTitle("<col=ff9040>Warnings</col>"));
        assertFalse(WarningWidgets.isWarningTitle("Animation"));
        assertFalse(WarningWidgets.isOverlayTitle("Animation"));
        assertFalse(WarningWidgets.isWarningTitle("Travel"));
        assertFalse(WarningWidgets.isOverlayTitle("Travel"));
    }

    @Test
    void aBareWarningTitleDoesNotTreatLongerPhrasesAsTheEntranaTitle() {
        assertFalse(WarningWidgets.isWarningTitle("Wilderness Warning"));
        assertFalse(WarningWidgets.isWarningTitle("This is a WARNING overlay"));
        assertFalse(WarningWidgets.isWarningTitle(""));
        assertFalse(WarningWidgets.isWarningTitle(null));
    }

    @Test
    void wildernessAndBodyTextCountAsOverlayTitles() {
        assertTrue(WarningWidgets.isOverlayTitle("Wilderness Warning"));
        assertTrue(WarningWidgets.isOverlayTitle("<col=ff0000>Wilderness Warning</col>"));
        assertTrue(WarningWidgets.isOverlayTitle("This is a WARNING overlay"));
        assertFalse(WarningWidgets.isOverlayTitle(""));
        assertFalse(WarningWidgets.isOverlayTitle(null));
    }

    @Test
    void aUniverseParentNameIsRecognised() {
        assertTrue(WarningWidgets.isUniverseInterface("universe"));
        assertTrue(WarningWidgets.isUniverseInterface("CwsWarning10.UNIVERSE"));
        assertFalse(WarningWidgets.isUniverseInterface("Warnings"));
        assertFalse(WarningWidgets.isUniverseInterface(null));
    }
}
