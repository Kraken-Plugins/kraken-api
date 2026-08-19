package unit.com.kraken.api.service.walker.transport;

import com.kraken.api.service.walker.transport.ObjectInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers turning a transport's {@code objectInfo} string into something clickable.
 *
 * <p>Every string here is taken verbatim from the transport dataset. The awkward cases are the ones
 * where the menu option runs to more than one word, because the dataset provides no delimiter between
 * the option and the target and a naive first-word split gets them wrong.</p>
 */
class ObjectInfoTest {

    @Test
    void aNullStringYieldsNothing() {
        assertNull(ObjectInfo.parse(null));
    }

    @Test
    void aBlankStringYieldsNothing() {
        assertNull(ObjectInfo.parse("   "));
    }

    @Test
    void aSimpleEntrySplitsIntoOptionTargetAndId() {
        ObjectInfo info = ObjectInfo.parse("Open Door 9398");

        assertEquals("Open", info.getMenuOption());
        assertEquals("Door", info.getMenuTarget());
        assertEquals(9398, info.getId());
        assertTrue(info.hasId());
    }

    @Test
    void aMultiWordTargetStaysWithTheTarget() {
        ObjectInfo info = ObjectInfo.parse("Board Swamp Boaty 6970");

        assertEquals("Board", info.getMenuOption());
        assertEquals("Swamp Boaty", info.getMenuTarget());
        assertEquals(6970, info.getId());
    }

    @Test
    void anEntryWithoutATrailingIdStillParses() {
        ObjectInfo info = ObjectInfo.parse("Open Odd-looking wall");

        assertEquals("Open", info.getMenuOption());
        assertEquals("Odd-looking wall", info.getMenuTarget());
        assertEquals(ObjectInfo.NO_ID, info.getId());
        assertFalse(info.hasId());
    }

    @Test
    void aSingleWordEntryHasNoTarget() {
        ObjectInfo info = ObjectInfo.parse("Enter 1234");

        assertEquals("Enter", info.getMenuOption());
        assertEquals("", info.getMenuTarget());
        assertEquals(1234, info.getId());
    }

    @Test
    void theEntityNameRecoversAMultiWordOption() {
        ObjectInfo info = ObjectInfo.parse("Al Kharid Amulet of Glory 13523")
                .withEntityName("Amulet of Glory");

        assertEquals("Al Kharid", info.getMenuOption());
        assertEquals("Amulet of Glory", info.getMenuTarget());
        assertEquals(13523, info.getId());
    }

    @Test
    void theEntityNameRecoversAnNpcDestinationOption() {
        ObjectInfo info = ObjectInfo.parse("Brimhaven Captain Barnaby 8763")
                .withEntityName("Captain Barnaby");

        assertEquals("Brimhaven", info.getMenuOption());
        assertEquals("Captain Barnaby", info.getMenuTarget());
    }

    @Test
    void theEntityNameHandlesARepeatedWord() {
        ObjectInfo info = ObjectInfo.parse("Camelot Camelot Portal 33094")
                .withEntityName("Camelot Portal");

        assertEquals("Camelot", info.getMenuOption());
        assertEquals("Camelot Portal", info.getMenuTarget());
    }

    @Test
    void theEntityNameLeavesAnAlreadyCorrectSplitAlone() {
        ObjectInfo info = ObjectInfo.parse("Configure Fairy ring 29560")
                .withEntityName("Fairy ring");

        assertEquals("Configure", info.getMenuOption());
        assertEquals("Fairy ring", info.getMenuTarget());
    }

    @Test
    void anEntityNameThatIsTheWholeRemainderLeavesNoOption() {
        ObjectInfo info = ObjectInfo.parse("Fairy ring 29560")
                .withEntityName("Fairy ring");

        assertEquals("", info.getMenuOption());
        assertEquals("Fairy ring", info.getMenuTarget());
    }

    @Test
    void aMismatchedEntityNameIsIgnored() {
        ObjectInfo parsed = ObjectInfo.parse("Open Door 9398");
        ObjectInfo refined = parsed.withEntityName("Gate");

        assertEquals(parsed, refined);
    }

    @Test
    void aNullEntityNameIsIgnored() {
        ObjectInfo parsed = ObjectInfo.parse("Open Door 9398");

        assertEquals(parsed, parsed.withEntityName(null));
    }

    @Test
    void theEntityNameMatchIsCaseInsensitive() {
        ObjectInfo info = ObjectInfo.parse("Travel Spirit tree 26261")
                .withEntityName("Spirit Tree");

        assertEquals("Travel", info.getMenuOption());
    }

    @Test
    void aTrailingNumberThatIsNotAnIdIsNotMistakenForOne() {
        ObjectInfo info = ObjectInfo.parse("Climb-up Staircase");

        assertEquals(ObjectInfo.NO_ID, info.getId());
        assertEquals("Climb-up", info.getMenuOption());
        assertEquals("Staircase", info.getMenuTarget());
    }

    @Test
    void theRemainderIsRecoverable() {
        assertEquals("Al Kharid Amulet of Glory", ObjectInfo.parse("Al Kharid Amulet of Glory 13523").getRemainder());
    }

    @Test
    void aStaleShipIdStillNamesTheSailor() {
        ObjectInfo info = ObjectInfo.parse("Musa Point Captain Tobias 14979");

        assertEquals("Musa", info.getMenuOption());
        assertEquals("Point Captain Tobias", info.getMenuTarget());
        assertTrue(info.namesEntity("Captain Tobias"));
        assertFalse(info.namesEntity("Seaman Lorris"));
    }

    @Test
    void aCustomsOfficerRowNamesTheOfficer() {
        ObjectInfo info = ObjectInfo.parse("Port Sarim Customs officer 14985");

        assertTrue(info.namesEntity("Customs officer"));
        assertFalse(info.namesEntity("Captain Tobias"));
    }
}
