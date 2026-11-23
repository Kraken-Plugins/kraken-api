package unit.com.kraken.api.util;

import com.kraken.api.util.StringUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class StringUtilsTest {

    @Test
    void twoSpellingsOfOneNameReduceToTheSameLettersAndDigits() {
        assertEquals("theclocktower", StringUtils.lettersAndDigits("The Clock Tower"));
        assertEquals(StringUtils.lettersAndDigits("The Clocktower"), StringUtils.lettersAndDigits("The Clock Tower"));
        assertEquals(StringUtils.lettersAndDigits("Giants' Foundry"), StringUtils.lettersAndDigits("Giant's Foundry"));
        assertEquals("karhewo", StringUtils.lettersAndDigits("Kar-Hewo"));
    }

    @Test
    void digitsAreKeptAndDifferentNamesStayDifferent() {
        assertEquals("level13wilderness", StringUtils.lettersAndDigits("Level 13 Wilderness"));
        assertNotEquals(StringUtils.lettersAndDigits("Tree Gnome Village"), StringUtils.lettersAndDigits("Tree Gnome Stronghold"));
    }

    @Test
    void nothingReducesToNothing() {
        assertEquals("", StringUtils.lettersAndDigits(null));
        assertEquals("", StringUtils.lettersAndDigits("  -' "));
    }
}
