package unit.com.kraken.api.service.walker.transport;

import com.kraken.api.service.walker.transport.TransportContext;
import com.kraken.api.service.walker.transport.handler.ItemTeleportHandler;
import com.kraken.api.service.walker.transport.registry.TransportDefinition;
import net.runelite.api.coords.WorldPoint;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers how a teleport item's live menu decides the click.
 *
 * <p>The item definition lists worn destinations even when the inventory widget only offers Rub,
 * so the live widget menu is what decides. A definition names its stop and its action in fields of
 * their own.</p>
 */
class ItemTeleportHandlerTest {

    @Test
    void anInventoryGloryDoesNotListWornDestinations() {
        String[] inventory = {"Wear", "Rub", "Drop", "Examine"};
        assertFalse(ItemTeleportHandler.hasLiveAction(inventory, "Karamja"));
        assertTrue(ItemTeleportHandler.hasLiveAction(inventory, "Rub"));
    }

    @Test
    void aWornGloryListsTheStopOnTheEquipmentWidget() {
        String[] worn = {"Remove", "Karamja", "Draynor Village", "Al Kharid", "Edgeville", "Falador"};
        assertTrue(ItemTeleportHandler.hasLiveAction(worn, "Karamja"));
        assertFalse(ItemTeleportHandler.hasLiveAction(worn, "Rub"));
    }

    @Test
    void aRegisteredTeleportListsItsOwnItemsToLookFor() {
        TransportDefinition glory = TransportDefinition
                .itemTeleport("test:glory", new WorldPoint(3087, 3496, 0), 1712, 1710, 1708)
                .build();
        TransportContext context = TransportContext.builder().definition(glory).build();

        assertEquals(List.of(1712, 1710, 1708), ItemTeleportHandler.candidateItemIds(context));
    }

    @Test
    void aTransportThatNamesNoItemHasNoCandidates() {
        assertTrue(ItemTeleportHandler.candidateItemIds(TransportContext.builder().build()).isEmpty());
    }

    @Test
    void theActionADefinitionNamesIsTriedBeforeTheUsualOnes() {
        List<String> menus = ItemTeleportHandler.menusToTry("Commune");

        assertEquals("Commune", menus.get(0));
        assertTrue(menus.contains("Rub") && menus.contains("Break"), menus.toString());
    }

    @Test
    void anActionThatIsAlreadyAUsualOneIsNotTriedTwice() {
        List<String> menus = ItemTeleportHandler.menusToTry("break");

        assertEquals("break", menus.get(0));
        assertEquals(1, menus.stream().filter("Break"::equalsIgnoreCase).count());
        assertEquals(menus.size(), ItemTeleportHandler.menusToTry(null).size());
        assertEquals(menus.size(), ItemTeleportHandler.menusToTry(" ").size());
    }

    @Test
    void aMissingWidgetMenuOffersNothing() {
        assertFalse(ItemTeleportHandler.hasLiveAction(null, "Karamja"));
        assertFalse(ItemTeleportHandler.hasLiveAction(new String[]{"Rub"}, null));
        assertFalse(ItemTeleportHandler.hasLiveAction(new String[]{"Rub"}, ""));
    }
}
