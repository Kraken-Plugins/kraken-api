package com.kraken.api.service.walker.transport.registry;

import com.kraken.api.service.pathfinding.GlobalPathfinderConfig;
import com.kraken.api.service.walker.transport.TransportShape;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers what a kind of transport decides: the planning option that turns it on and off, and the
 * way it is operated when a definition states no shape.
 */
class TransportCategoryTest {

    private static final GlobalPathfinderConfig DEFAULTS = GlobalPathfinderConfig.builder().build();

    @Test
    void everyCategoryHasAShapeAndIsOnByDefault() {
        for (TransportCategory category : TransportCategory.values()) {
            assertNotNull(category.getDefaultShape(), category.name());
            assertTrue(category.isEnabled(DEFAULTS), category.name());
            assertTrue(category.isEnabled(null), category.name() + " with no options is planned");
        }
    }

    @Test
    void aCategoryIsOperatedTheWayItsTransportsWork() {
        assertEquals(TransportShape.SINGLE_CLICK, TransportCategory.GENERAL.getDefaultShape());
        assertEquals(TransportShape.SINGLE_CLICK, TransportCategory.AGILITY_SHORTCUT.getDefaultShape());
        assertEquals(TransportShape.SINGLE_CLICK, TransportCategory.TELEPORTATION_PORTAL.getDefaultShape());
        assertEquals(TransportShape.CLICK_THEN_DIALOGUE, TransportCategory.SHIP.getDefaultShape());
        assertEquals(TransportShape.CLICK_THEN_DIALOGUE, TransportCategory.CHARTER_SHIP.getDefaultShape());
        assertEquals(TransportShape.FAIRY_RING, TransportCategory.FAIRY_RING.getDefaultShape());
        assertEquals(TransportShape.HUB_RESUME_PAUSE, TransportCategory.SPIRIT_TREE.getDefaultShape());
        assertEquals(TransportShape.HUB_RESUME_PAUSE, TransportCategory.MINECART.getDefaultShape());
        assertEquals(TransportShape.HUB_DIALOGUE, TransportCategory.WILDERNESS_OBELISK.getDefaultShape());
        assertEquals(TransportShape.CLICK_THEN_WIDGET, TransportCategory.GNOME_GLIDER.getDefaultShape());
        assertEquals(TransportShape.CLICK_THEN_WIDGET, TransportCategory.QUETZAL.getDefaultShape());
        assertEquals(TransportShape.ITEM_SUBOP, TransportCategory.TELEPORTATION_ITEM.getDefaultShape());
        assertEquals(TransportShape.ITEM_SUBOP, TransportCategory.QUETZAL_WHISTLE.getDefaultShape());
        assertEquals(TransportShape.SPELL, TransportCategory.TELEPORTATION_SPELL.getDefaultShape());
        assertEquals(TransportShape.SPELL, TransportCategory.TELEPORTATION_SPELL_HOME.getDefaultShape());
        assertEquals(TransportShape.CANOE, TransportCategory.CANOE.getDefaultShape());
        assertEquals(TransportShape.GROUPING_TELEPORT, TransportCategory.TELEPORTATION_MINIGAME.getDefaultShape());
    }

    @Test
    void aPlanningOptionTurnsItsCategoryOff() {
        assertFalse(TransportCategory.BOAT.isEnabled(GlobalPathfinderConfig.builder().useBoats(false).build()));
        assertFalse(TransportCategory.FAIRY_RING.isEnabled(GlobalPathfinderConfig.builder().useFairyRings(false).build()));
        assertFalse(TransportCategory.TELEPORTATION_SPELL.isEnabled(
                GlobalPathfinderConfig.builder().useTeleportationSpells(false).build()));
        assertTrue(TransportCategory.GENERAL.isEnabled(GlobalPathfinderConfig.builder().useBoats(false).build()));
    }

    @Test
    void teleportsAnItemCarriesAllFollowTheItemOption() {
        GlobalPathfinderConfig noItems = GlobalPathfinderConfig.builder().useTeleportationItems(false).build();

        assertFalse(TransportCategory.TELEPORTATION_ITEM.isEnabled(noItems));
        assertFalse(TransportCategory.QUETZAL_WHISTLE.isEnabled(noItems));
        assertFalse(TransportCategory.SEASONAL.isEnabled(noItems));
        assertFalse(TransportCategory.TELEPORTATION_BOX.isEnabled(noItems));
        assertTrue(TransportCategory.QUETZAL.isEnabled(noItems), "a quetzal flown from its landing site needs no item");
        assertFalse(TransportCategory.QUETZAL_WHISTLE.isEnabled(GlobalPathfinderConfig.builder().useQuetzals(false).build()));
    }

    @Test
    void thePlayerOwnedHouseCategoriesFollowTheirOwnOptions() {
        assertFalse(TransportCategory.TELEPORTATION_BOX.isEnabled(
                GlobalPathfinderConfig.builder().useTeleportationBoxes(false).build()));
        assertFalse(TransportCategory.TELEPORTATION_PORTAL_POH.isEnabled(
                GlobalPathfinderConfig.builder().useTeleportationPortalsPoh(false).build()));
        assertTrue(TransportCategory.TELEPORTATION_PORTAL.isEnabled(
                GlobalPathfinderConfig.builder().useTeleportationPortalsPoh(false).build()));
    }

    @Test
    void noOptionTurnsTheHomeTeleportOff() {
        assertTrue(TransportCategory.TELEPORTATION_SPELL_HOME.isEnabled(
                GlobalPathfinderConfig.builder().useTeleportationSpells(false).build()));
    }

    @Test
    void onlyQuetzalsAndTheirWhistleCompeteForTheSameDestinations() {
        for (TransportCategory category : TransportCategory.values()) {
            boolean quetzal = category == TransportCategory.QUETZAL || category == TransportCategory.QUETZAL_WHISTLE;
            assertEquals(quetzal, category.sharesDestinations(), category.name());
            assertEquals(category == TransportCategory.QUETZAL_WHISTLE ? 15 : 0, category.getDifferentialCost(),
                    category.name());
        }
    }

    @Test
    void aDefinitionThatNamesOnlyItsCategoryTakesTheUsualShape() {
        TransportDefinition ship = TransportDefinition.builder()
                .id("test:ship")
                .origin(new net.runelite.api.coords.WorldPoint(3029, 3217, 0))
                .destination(new net.runelite.api.coords.WorldPoint(2956, 3146, 0))
                .category(TransportCategory.SHIP)
                .objectInfo("Travel Captain Tobias 14978")
                .build();

        assertEquals(TransportShape.CLICK_THEN_DIALOGUE, ship.getShape());
    }
}
