package unit.com.kraken.api.service.walker.transport;

import com.kraken.api.service.walker.transport.CanoeWidgets;
import com.kraken.api.service.walker.transport.TransportShape;
import com.kraken.api.service.walker.transport.registry.TransportDataset;
import com.kraken.api.service.walker.transport.registry.TransportDefinition;
import net.runelite.api.gameval.InterfaceID;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers picking the action to send to the canoe shaping interface and the river maps.
 *
 * <p>The sweep matters most: every canoe of the dataset names a stop, and the walker finds that
 * stop by the action its map button offers. A regenerated dataset that spells a stop in a way no
 * button matches fails here instead of at a canoe station.</p>
 */
class CanoeWidgetsTest {

    /**
     * What the buttons of the two river maps offer for a stop the canoe reaches, as the game's
     * client scripts build them (revision 242): the River Lum, then the river through Ardougne.
     */
    private static final List<String> MAP_ACTIONS = List.of(
            "Travel to<col=ff9040> Lumbridge",
            "Travel to<col=ff9040> Champions Guild",
            "Travel to<col=ff9040> Barbarian Village",
            "Travel to<col=ff9040> Edgeville",
            "Travel to<col=ff9040> Ferox Enclave",
            "Travel to<col=ff9040> Wilderness Pond",
            "Travel to<col=ff9040> Castle Wars",
            "Travel to<col=ff9040> Tree Gnome Village",
            "Travel to<col=ff9040> The Clocktower",
            "Travel to<col=ff9040> Chaos Druid Tower",
            "Travel to<col=ff9040> Tree Gnome Stronghold");

    private static List<String> buttonsLeadingTo(String stop) {
        return MAP_ACTIONS.stream()
                .filter(action -> CanoeWidgets.travelAction(new String[]{action}, stop) != null)
                .collect(Collectors.toList());
    }

    @Test
    void everyCanoeOfTheDatasetFindsItsStopOnExactlyOneMapButton() {
        List<String> problems = new ArrayList<>();
        int canoes = 0;
        for (TransportDefinition definition : TransportDataset.bundled()) {
            if (definition.getShape() != TransportShape.CANOE) {
                continue;
            }
            canoes++;

            List<String> buttons = buttonsLeadingTo(definition.getDisplayInfo());
            if (buttons.size() != 1) {
                problems.add(definition.getId() + " '" + definition.getDisplayInfo() + "' matches " + buttons);
            }
        }

        assertTrue(canoes > 0, "the dataset lists no canoes");
        assertTrue(problems.isEmpty(), problems.toString());
    }

    @Test
    void aStopIsFoundWhateverItsSpellingAndTheButtonsOwnActionIsReturned() {
        String clockTower = "Travel to<col=ff9040> The Clocktower";

        assertEquals(clockTower, CanoeWidgets.travelAction(new String[]{null, clockTower}, "The Clock Tower"));
        assertEquals(clockTower, CanoeWidgets.travelAction(new String[]{clockTower}, "the clocktower"));
        assertEquals("Travel to Edgeville", CanoeWidgets.travelAction(new String[]{"Travel to Edgeville"}, "Edgeville"));
    }

    @Test
    void aStopTheCanoeCannotReachOffersNoActionToFind() {
        assertNull(CanoeWidgets.travelAction(new String[]{""}, "Lumbridge"), "an unreachable stop has an empty action");
        assertNull(CanoeWidgets.travelAction(new String[]{null}, "Lumbridge"));
        assertNull(CanoeWidgets.travelAction(null, "Lumbridge"));
        assertNull(CanoeWidgets.travelAction(new String[]{"Close"}, "Lumbridge"));
    }

    @Test
    void aStopIsNotMistakenForAnother() {
        String village = "Travel to<col=ff9040> Tree Gnome Village";

        assertNull(CanoeWidgets.travelAction(new String[]{village}, "Tree Gnome Stronghold"));
        assertNull(CanoeWidgets.travelAction(new String[]{village}, "Tree Gnome"));
        assertNull(CanoeWidgets.travelAction(new String[]{village}, ""));
        assertNull(CanoeWidgets.travelAction(new String[]{village}, null));
    }

    @Test
    void theMakeActionOfAHullButtonIsFound() {
        String waka = "Make<col=ff9040> Waka canoe";

        assertEquals(waka, CanoeWidgets.makeAction(new String[]{waka}));
        assertEquals("Make Log canoe", CanoeWidgets.makeAction(new String[]{null, "Make Log canoe"}));
        assertNull(CanoeWidgets.makeAction(new String[]{"Close"}));
        assertNull(CanoeWidgets.makeAction(new String[]{}));
        assertNull(CanoeWidgets.makeAction(null));
    }

    @Test
    void theLargestHullIsTriedFirst() {
        assertArrayEquals(new int[]{
                InterfaceID.Canoeing.WAKA,
                InterfaceID.Canoeing.STABLE_DUGOUT,
                InterfaceID.Canoeing.DUGOUT,
                InterfaceID.Canoeing.LOG
        }, CanoeWidgets.hullLayers());
    }

    @Test
    void bothRiverMapsAreLookedAtAndTheListsCannotBeChangedByACaller() {
        assertArrayEquals(new int[]{
                InterfaceID.CanoeMapLum.DESTINATIONS,
                InterfaceID.CanoeMapDougne.DESTINATIONS
        }, CanoeWidgets.destinationLayers());

        assertNotSame(CanoeWidgets.hullLayers(), CanoeWidgets.hullLayers());
        CanoeWidgets.destinationLayers()[0] = -1;
        assertFalse(CanoeWidgets.destinationLayers()[0] == -1);
    }
}
