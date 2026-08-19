package unit.com.kraken.api.service.walker.transport;

import com.kraken.api.service.walker.transport.TransportShape;
import com.kraken.api.service.walker.transport.TransportShapes;
import org.junit.jupiter.api.Test;
import shortestpath.transport.TransportType;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the mapping from transport type to the way it is operated.
 *
 * <p>The exhaustiveness check is the point of this class: the transport dataset is a pinned external
 * dependency, so a bump that introduces a new kind of transport should fail here rather than in the
 * middle of a walk.</p>
 */
class TransportShapesTest {

    @Test
    void everyTransportTypeHasAShape() {
        List<TransportType> unmapped = new ArrayList<>();
        for (TransportType type : TransportType.values()) {
            if (!TransportShapes.isMapped(type)) {
                unmapped.add(type);
            }
        }

        assertTrue(unmapped.isEmpty(), "no execution shape mapped for: " + unmapped);
    }

    @Test
    void aDoorIsASingleClick() {
        assertEquals(TransportShape.SINGLE_CLICK, TransportShapes.of(TransportType.TRANSPORT));
    }

    @Test
    void aBoatOpensAConversation() {
        assertEquals(TransportShape.CLICK_THEN_DIALOGUE, TransportShapes.of(TransportType.BOAT));
    }

    @Test
    void fairyRingsAreSelectedByCode() {
        assertEquals(TransportShape.FAIRY_RING, TransportShapes.of(TransportType.FAIRY_RING));
    }

    @Test
    void spiritTreesOfferTheirStopsAsChatOptions() {
        assertEquals(TransportShape.HUB_DIALOGUE, TransportShapes.of(TransportType.SPIRIT_TREE));
    }

    @Test
    void hubsWithADedicatedInterfaceSelectOnIt() {
        assertEquals(TransportShape.CLICK_THEN_WIDGET, TransportShapes.of(TransportType.GNOME_GLIDER));
        assertEquals(TransportShape.CLICK_THEN_WIDGET, TransportShapes.of(TransportType.HOT_AIR_BALLOON));
        assertEquals(TransportShape.CLICK_THEN_WIDGET, TransportShapes.of(TransportType.QUETZAL));
    }

    @Test
    void hubsWithNumberedChatOptionsShareTheDialogueShape() {
        assertEquals(TransportShape.HUB_DIALOGUE, TransportShapes.of(TransportType.MINECART));
        assertEquals(TransportShape.HUB_DIALOGUE, TransportShapes.of(TransportType.WILDERNESS_OBELISK));
    }

    @Test
    void theTwoUnimplementedKindsKeepTheirOwnShapes() {
        assertEquals(TransportShape.CANOE, TransportShapes.of(TransportType.CANOE));
        assertEquals(TransportShape.GROUPING_TELEPORT, TransportShapes.of(TransportType.TELEPORTATION_MINIGAME));
    }

    @Test
    void teleportItemsAreUsedFromTheInventory() {
        assertEquals(TransportShape.ITEM_SUBOP, TransportShapes.of(TransportType.TELEPORTATION_ITEM));
    }

    @Test
    void teleportSpellsAreCast() {
        assertEquals(TransportShape.SPELL, TransportShapes.of(TransportType.TELEPORTATION_SPELL));
        assertEquals(TransportShape.SPELL, TransportShapes.of(TransportType.TELEPORTATION_SPELL_HOME));
    }

    @Test
    void anUnknownTypeHasNoShape() {
        assertNull(TransportShapes.of(null));
    }
}
