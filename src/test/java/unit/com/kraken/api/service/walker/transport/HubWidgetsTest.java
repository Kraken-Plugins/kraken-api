package unit.com.kraken.api.service.walker.transport;

import com.kraken.api.service.walker.transport.DisplayInfo;
import com.kraken.api.service.walker.transport.HubWidgets;
import org.junit.jupiter.api.Test;
import com.kraken.api.service.walker.transport.registry.TransportCategory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers resolving a hub destination to the component that selects it.
 *
 * <p>The sweeps matter more than the individual cases: every destination the dataset offers for these
 * hubs must resolve, and the ids come from the client by name, so a client update that renames a
 * component fails here rather than in the middle of a walk.</p>
 */
class HubWidgetsTest {

    /** Every gnome glider stop, as the dataset names them. */
    private static final List<String> GLIDER_STOPS = Arrays.asList(
            "Gandius", "Kar-Hewo", "Lemanto Andra", "Lemantolly Undri",
            "Ookookolly Undri", "Sindarpos", "Ta Quir Priw");

    /** Every hot air balloon stop, as the dataset names them. */
    private static final List<String> BALLOON_STOPS = Arrays.asList(
            "Castle Wars", "Crafting Guild", "Entrana", "Grand Tree", "Taverley", "Varrock");

    /** Every magic mushtree stop, as the dataset writes them. */
    private static final List<String> MUSHTREE_STOPS = Arrays.asList(
            "1. House on the Hill", "2. Verdant Valley", "3. Sticky Swamp", "4. Mushroom Meadow");

    private static int resolve(TransportCategory type, String displayInfo) {
        return HubWidgets.component(type, DisplayInfo.parse(displayInfo));
    }

    @Test
    void everyGliderStopResolves() {
        List<String> missing = new ArrayList<>();
        for (String stop : GLIDER_STOPS) {
            if (resolve(TransportCategory.GNOME_GLIDER, stop) == HubWidgets.NOT_FOUND) {
                missing.add(stop);
            }
        }

        assertTrue(missing.isEmpty(), "no component for glider stops: " + missing);
    }

    @Test
    void everyBalloonStopResolves() {
        List<String> missing = new ArrayList<>();
        for (String stop : BALLOON_STOPS) {
            if (resolve(TransportCategory.HOT_AIR_BALLOON, stop) == HubWidgets.NOT_FOUND) {
                missing.add(stop);
            }
        }

        assertTrue(missing.isEmpty(), "no component for balloon stops: " + missing);
    }

    @Test
    void everyMushtreeStopResolves() {
        List<String> missing = new ArrayList<>();
        for (String stop : MUSHTREE_STOPS) {
            if (resolve(TransportCategory.MAGIC_MUSHTREE, stop) == HubWidgets.NOT_FOUND) {
                missing.add(stop);
            }
        }

        assertTrue(missing.isEmpty(), "no component for mushtree stops: " + missing);
    }

    @Test
    void distinctStopsResolveToDistinctComponents() {
        assertNotEquals(resolve(TransportCategory.GNOME_GLIDER, "Gandius"),
                resolve(TransportCategory.GNOME_GLIDER, "Sindarpos"));
        assertNotEquals(resolve(TransportCategory.HOT_AIR_BALLOON, "Entrana"),
                resolve(TransportCategory.HOT_AIR_BALLOON, "Taverley"));
        assertNotEquals(resolve(TransportCategory.MAGIC_MUSHTREE, "1. House on the Hill"),
                resolve(TransportCategory.MAGIC_MUSHTREE, "4. Mushroom Meadow"));
    }

    @Test
    void stopNamesAreMatchedLoosely() {
        assertEquals(resolve(TransportCategory.GNOME_GLIDER, "Kar-Hewo"),
                resolve(TransportCategory.GNOME_GLIDER, "kar hewo"));
        assertEquals(resolve(TransportCategory.HOT_AIR_BALLOON, "Castle Wars"),
                resolve(TransportCategory.HOT_AIR_BALLOON, "castlewars"));
    }

    @Test
    void anUnknownStopDoesNotResolve() {
        assertEquals(HubWidgets.NOT_FOUND, resolve(TransportCategory.GNOME_GLIDER, "Nowhere"));
        assertEquals(HubWidgets.NOT_FOUND, resolve(TransportCategory.HOT_AIR_BALLOON, "Nowhere"));
    }

    @Test
    void nullsDoNotResolve() {
        assertEquals(HubWidgets.NOT_FOUND, HubWidgets.component(null, null));
        assertEquals(HubWidgets.NOT_FOUND, HubWidgets.component(TransportCategory.GNOME_GLIDER, null));
    }

    @Test
    void quetzalsAreSelectedByReadingTheList() {
        assertFalse(HubWidgets.hasFixedComponents(TransportCategory.QUETZAL));
        assertNotEquals(HubWidgets.NOT_FOUND, HubWidgets.textInterfaceGroup(TransportCategory.QUETZAL));
    }

    @Test
    void hubsWithFixedComponentsAreFlagged() {
        assertTrue(HubWidgets.hasFixedComponents(TransportCategory.GNOME_GLIDER));
        assertTrue(HubWidgets.hasFixedComponents(TransportCategory.HOT_AIR_BALLOON));
        assertTrue(HubWidgets.hasFixedComponents(TransportCategory.MAGIC_MUSHTREE));
        assertFalse(HubWidgets.hasFixedComponents(TransportCategory.GENERAL));
    }
}
