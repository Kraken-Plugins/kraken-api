package unit.com.kraken.api.service.walker.transport;

import com.kraken.api.service.magic.CastableSpell;
import com.kraken.api.service.magic.spellbook.Standard;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Covers matching a teleport transport's display info to a spell.
 *
 * <p>Teleport spells are the one transport category the dataset describes only in prose, so this
 * mapping is the whole of their execution data.</p>
 */
class SpellTeleportMatchingTest {

    private static CastableSpell find(String displayInfo) throws Exception {
        Class<?> handler = Class.forName("com.kraken.api.service.walker.transport.handler.SpellTeleportHandler");
        Method method = handler.getDeclaredMethod("findSpell", String.class);
        method.setAccessible(true);
        return (CastableSpell) method.invoke(null, displayInfo);
    }

    @Test
    void aPlainSpellNameMatches() throws Exception {
        assertEquals(Standard.VARROCK_TELEPORT, find("Varrock Teleport"));
    }

    @Test
    void spacingAndUnderscoresAreIgnored() throws Exception {
        assertEquals(Standard.LUMBRIDGE_TELEPORT, find("Lumbridge Teleport"));
        assertEquals(Standard.FALADOR_TELEPORT, find("Falador Teleport"));
        assertEquals(Standard.CAMELOT_TELEPORT, find("Camelot Teleport"));
    }

    @Test
    void aMultiWordSpellMatches() throws Exception {
        assertEquals(Standard.TELEPORT_TO_HOUSE, find("Teleport to House"));
    }

    @Test
    void aQualifiedDestinationFallsBackToTheBaseSpell() throws Exception {
        assertEquals(Standard.VARROCK_TELEPORT, find("Varrock Teleport: GE"));
    }

    @Test
    void theHomeTeleportMatches() throws Exception {
        assertNotNull(find("Home Teleport"));
    }

    @Test
    void anUnknownSpellMatchesNothing() throws Exception {
        assertNull(find("Definitely Not A Spell"));
    }

    @Test
    void nullAndBlankMatchNothing() throws Exception {
        assertNull(find(null));
        assertNull(find("   "));
    }
}
