package unit.com.kraken.api.service.walker.transport;

import com.kraken.api.service.walker.transport.FairyRingWidgets;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers resolving fairy ring codes to the components that select them.
 *
 * <p>The exhaustive sweep is the point: the ids are looked up from the client by name rather than
 * copied into this repository, so this fails if a client update renames or drops one.</p>
 */
class FairyRingWidgetsTest {

    private static final char[] FIRST_DIAL = {'A', 'B', 'C', 'D'};
    private static final char[] SECOND_DIAL = {'I', 'J', 'K', 'L'};
    private static final char[] THIRD_DIAL = {'P', 'Q', 'R', 'S'};

    @Test
    void everyCodeResolvesToATravelLogEntry() {
        List<String> missing = new ArrayList<>();

        for (char first : FIRST_DIAL) {
            for (char second : SECOND_DIAL) {
                for (char third : THIRD_DIAL) {
                    String code = "" + first + second + third;
                    if (FairyRingWidgets.logEntry(code) == FairyRingWidgets.NOT_FOUND) {
                        missing.add(code);
                    }
                }
            }
        }

        assertTrue(missing.isEmpty(), "no travel log component for: " + missing);
    }

    @Test
    void thereAreSixtyFourCodes() {
        assertEquals(64, FIRST_DIAL.length * SECOND_DIAL.length * THIRD_DIAL.length);
    }

    @Test
    void everyDialLetterResolves() {
        List<Character> missing = new ArrayList<>();
        for (char[] dial : new char[][]{FIRST_DIAL, SECOND_DIAL, THIRD_DIAL}) {
            for (char letter : dial) {
                if (FairyRingWidgets.dialLetter(letter) == FairyRingWidgets.NOT_FOUND) {
                    missing.add(letter);
                }
            }
        }

        assertTrue(missing.isEmpty(), "no dial component for: " + missing);
    }

    @Test
    void distinctCodesResolveToDistinctComponents() {
        assertNotEquals(FairyRingWidgets.logEntry("AIQ"), FairyRingWidgets.logEntry("AIR"));
        assertNotEquals(FairyRingWidgets.logEntry("AIQ"), FairyRingWidgets.logEntry("BIQ"));
    }

    @Test
    void lookupIsCaseInsensitive() {
        assertEquals(FairyRingWidgets.logEntry("ALQ"), FairyRingWidgets.logEntry("alq"));
    }

    @Test
    void malformedCodesAreRejected() {
        assertFalse(FairyRingWidgets.isValidCode(null));
        assertFalse(FairyRingWidgets.isValidCode("AL"));
        assertFalse(FairyRingWidgets.isValidCode("ALQR"));
        assertFalse(FairyRingWidgets.isValidCode("XYZ"));
        assertFalse(FairyRingWidgets.isValidCode("AAA"));

        assertEquals(FairyRingWidgets.NOT_FOUND, FairyRingWidgets.logEntry("XYZ"));
        assertEquals(FairyRingWidgets.NOT_FOUND, FairyRingWidgets.logEntry(null));
    }

    @Test
    void wellFormedCodesAreAccepted() {
        assertTrue(FairyRingWidgets.isValidCode("AIP"));
        assertTrue(FairyRingWidgets.isValidCode("DLS"));
        assertTrue(FairyRingWidgets.isValidCode("blr"));
    }
}
