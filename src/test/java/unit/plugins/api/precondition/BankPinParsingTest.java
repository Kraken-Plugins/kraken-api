package unit.plugins.api.precondition;

import org.junit.jupiter.api.Test;
import plugins.api.precondition.BankHelper;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Covers bank PIN parsing.
 *
 * <p>Worth testing on its own because the failure is silent and expensive: a malformed PIN that
 * parsed into garbage digits would be typed at the keypad, and repeated wrong entries lock the bank.
 * Returning null instead lets the engine skip the test with a clear reason.</p>
 */
class BankPinParsingTest {

    @Test
    void aFourDigitPinParses() {
        assertArrayEquals(new int[]{1, 2, 3, 4}, BankHelper.parsePin("1234"));
    }

    @Test
    void leadingZeroesArepreserved() {
        assertArrayEquals(new int[]{0, 0, 0, 7}, BankHelper.parsePin("0007"));
    }

    @Test
    void surroundingWhitespaceIsIgnored() {
        assertArrayEquals(new int[]{9, 8, 7, 6}, BankHelper.parsePin("  9876  "));
    }

    @Test
    void anUnsetPinIsRejected() {
        assertNull(BankHelper.parsePin(null));
        assertNull(BankHelper.parsePin(""));
        assertNull(BankHelper.parsePin("   "));
    }

    @Test
    void wrongLengthIsRejected() {
        assertNull(BankHelper.parsePin("123"));
        assertNull(BankHelper.parsePin("12345"));
    }

    @Test
    void nonDigitsAreRejected() {
        assertNull(BankHelper.parsePin("12a4"));
        assertNull(BankHelper.parsePin("１２３４"));
        assertNull(BankHelper.parsePin("12 4"));
        assertNull(BankHelper.parsePin("-123"));
    }
}
