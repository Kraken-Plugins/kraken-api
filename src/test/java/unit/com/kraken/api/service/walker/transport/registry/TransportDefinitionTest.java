package unit.com.kraken.api.service.walker.transport.registry;

import com.kraken.api.service.pathfinding.GlobalPathfinderConfig;
import com.kraken.api.service.walker.transport.ObjectInfo;
import com.kraken.api.service.walker.transport.TransportShape;
import com.kraken.api.service.walker.transport.registry.DefinitionRequirements;
import com.kraken.api.service.walker.transport.registry.TransportCategory;
import com.kraken.api.service.walker.transport.registry.TransportDefinition;
import net.runelite.api.coords.WorldPoint;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers what a registered transport must say about itself before the walker will accept it.
 */
class TransportDefinitionTest {

    private static final WorldPoint ORIGIN = new WorldPoint(1312, 3685, 0);
    private static final WorldPoint DESTINATION = new WorldPoint(1312, 10086, 0);

    @Test
    void anObjectDefinitionIsASingleClickOnThatObject() {
        TransportDefinition definition =
                TransportDefinition.object("test:cave", ORIGIN, DESTINATION, 34405, "Enter").build();

        assertEquals(TransportShape.SINGLE_CLICK, definition.getShape());
        assertEquals("Enter 34405", definition.getObjectInfo());

        ObjectInfo info = definition.toObjectInfo();
        assertEquals("Enter", info.getMenuOption());
        assertEquals(34405, info.getId());
    }

    @Test
    void aMultiWordActionSurvivesWithoutBeingGuessedFromAJoinedString() {
        TransportDefinition definition = TransportDefinition.builder()
                .id("test:glory")
                .origin(ORIGIN)
                .destination(DESTINATION)
                .shape(TransportShape.SINGLE_CLICK)
                .menuOption("Al Kharid")
                .menuTarget("Amulet of Glory")
                .entityId(13523)
                .build();

        assertEquals("Al Kharid Amulet of Glory 13523", definition.getObjectInfo());
        assertEquals("Al Kharid", definition.toObjectInfo().getMenuOption());
        assertEquals("Amulet of Glory", definition.toObjectInfo().getMenuTarget());
    }

    @Test
    void anNpcDefinitionIsFoundByName() {
        TransportDefinition definition =
                TransportDefinition.npc("test:boatman", ORIGIN, DESTINATION, "Boatman", "Travel").build();

        assertEquals("Travel Boatman", definition.getObjectInfo());
        assertFalse(definition.toObjectInfo().hasId());
    }

    @Test
    void anNpcDialogueDefinitionCarriesTheOptionToChoose() {
        TransportDefinition definition = TransportDefinition
                .npcDialogue("test:veos", ORIGIN, DESTINATION, "Veos", "Talk-to", "Can you take me to Great Kourend?")
                .build();

        assertEquals(TransportShape.CLICK_THEN_DIALOGUE, definition.getShape());
        assertEquals("Can you take me to Great Kourend?", definition.getDisplayInfo());
    }

    @Test
    void defaultsAreAGeneralOneTickTransportWithNoRequirements() {
        TransportDefinition definition =
                TransportDefinition.object("test:door", ORIGIN, DESTINATION, 1, "Open").build();

        assertEquals(TransportCategory.GENERAL, definition.getCategory());
        assertEquals(1, definition.getDuration());
        assertSame(DefinitionRequirements.NONE, definition.getRequirements());
        assertFalse(definition.isBidirectional());
        assertFalse(definition.isReplaces());
        assertNull(definition.getHandler());
    }

    @Test
    void theOwnerIsThePartOfTheIdBeforeTheColon() {
        assertEquals("my-plugin",
                TransportDefinition.object("my-plugin:door", ORIGIN, DESTINATION, 1, "Open").build().getOwner());
        assertEquals("door",
                TransportDefinition.object("door", ORIGIN, DESTINATION, 1, "Open").build().getOwner());
    }

    @Test
    void aHandlerStandsInForAShapeAndAClickTarget() {
        TransportDefinition definition = TransportDefinition.builder()
                .id("test:custom")
                .origin(ORIGIN)
                .destination(DESTINATION)
                .handler(context -> true)
                .build();

        assertNull(definition.getShape());
        assertNull(definition.getObjectInfo());
        assertNotNull(definition.getHandler());
    }

    @Test
    void anIdAndADestinationAreRequiredAndAClickedTransportNeedsAnOrigin() {
        assertThrows(IllegalArgumentException.class,
                () -> TransportDefinition.object(" ", ORIGIN, DESTINATION, 1, "Open").build());
        assertThrows(IllegalArgumentException.class,
                () -> TransportDefinition.object("test:door", null, DESTINATION, 1, "Open").build());
        assertThrows(IllegalArgumentException.class,
                () -> TransportDefinition.object("test:door", ORIGIN, null, 1, "Open").build());
        assertThrows(IllegalArgumentException.class,
                () -> TransportDefinition.object("test:door", ORIGIN, ORIGIN, 1, "Open").build());
    }

    @Test
    void anItemTeleportIsUsableFromAnywhereAndRequiresOneOfItsItems() {
        TransportDefinition tablet = TransportDefinition.itemTeleport("test:varrock-tab", DESTINATION, 8007).build();

        assertTrue(tablet.isUsableAnywhere());
        assertNull(tablet.getOrigin());
        assertEquals(TransportShape.ITEM_SUBOP, tablet.getShape());
        assertEquals(TransportCategory.TELEPORTATION_ITEM, tablet.getCategory());
        assertEquals(List.of(8007), tablet.getItemIds());
        assertEquals(TransportDefinition.DEFAULT_MAX_WILDERNESS_LEVEL, tablet.getMaxWildernessLevel());
        assertEquals(1, tablet.getRequirements().getItems().size());
        assertArrayEquals(new int[]{8007}, tablet.getRequirements().getItems().get(0).getAnyOf());
        assertTrue(tablet.toString().contains("anywhere"), tablet.toString());
    }

    @Test
    void theItemsOfATeleportAreRequiredOnTopOfWhatItStates() {
        TransportDefinition glory = TransportDefinition.itemTeleport("test:glory", DESTINATION, 1712, 1710)
                .menuOption("Rub")
                .displayInfo("Edgeville")
                .maxWildernessLevel(30)
                .requirements(DefinitionRequirements.builder().membersOnly(true).build())
                .build();

        assertTrue(glory.getRequirements().isMembersOnly());
        assertArrayEquals(new int[]{1712, 1710}, glory.getRequirements().getItems().get(0).getAnyOf());
        assertEquals(1, glory.getRequirements().getItems().get(0).getQuantity());
        assertEquals(30, glory.getMaxWildernessLevel());
        assertEquals("Rub", glory.getObjectInfo());
        assertThrows(UnsupportedOperationException.class, () -> glory.getItemIds().add(1));
    }

    @Test
    void aSpellTeleportNamesItsSpellAndStatesNoRequirements() {
        TransportDefinition spell =
                TransportDefinition.spellTeleport("test:varrock", DESTINATION, "Varrock Teleport").build();

        assertTrue(spell.isUsableAnywhere());
        assertEquals(TransportShape.SPELL, spell.getShape());
        assertEquals(TransportCategory.TELEPORTATION_SPELL, spell.getCategory());
        assertEquals("Varrock Teleport", spell.getDisplayInfo());
        assertSame(DefinitionRequirements.NONE, spell.getRequirements());
        assertNull(spell.getObjectInfo());
    }

    @Test
    void aCategoryThatIsStatedIsKeptWhateverTheShape() {
        TransportDefinition whistle = TransportDefinition.itemTeleport("test:whistle", DESTINATION, 29271)
                .category(TransportCategory.QUETZAL)
                .build();

        assertEquals(TransportCategory.QUETZAL, whistle.getCategory());
    }

    @Test
    void aTeleportUsableFromAnywhereCannotClickTheSceneGoBackOrReplaceAnEdge() {
        assertThrows(IllegalArgumentException.class, () -> TransportDefinition.builder()
                .id("test:nowhere-door")
                .destination(DESTINATION)
                .shape(TransportShape.SINGLE_CLICK)
                .entityId(1535)
                .build());
        assertThrows(IllegalArgumentException.class,
                () -> TransportDefinition.itemTeleport("test:tab", DESTINATION, 8007).bidirectional(true).build());
        assertThrows(IllegalArgumentException.class,
                () -> TransportDefinition.itemTeleport("test:tab", DESTINATION, 8007).replaces(true).build());
        assertThrows(IllegalArgumentException.class,
                () -> TransportDefinition.itemTeleport("test:tab", DESTINATION, 8007).maxWildernessLevel(-1).build());
    }

    @Test
    void aDeclarativeTeleportMustNameItsItemsOrItsSpell() {
        assertThrows(IllegalArgumentException.class,
                () -> TransportDefinition.itemTeleport("test:no-items", DESTINATION).build());
        assertThrows(IllegalArgumentException.class,
                () -> TransportDefinition.itemTeleport("test:bad-item", DESTINATION, -1).build());
        assertThrows(IllegalArgumentException.class,
                () -> TransportDefinition.spellTeleport("test:no-spell", DESTINATION, " ").build());
    }

    @Test
    void aTeleportWithItsOwnHandlerNeedsNothingElse() {
        TransportDefinition custom = TransportDefinition.builder()
                .id("test:custom-teleport")
                .destination(DESTINATION)
                .handler(context -> true)
                .build();

        assertTrue(custom.isUsableAnywhere());
        assertEquals(TransportCategory.GENERAL, custom.getCategory());
        assertTrue(custom.getItemIds().isEmpty());
    }

    @Test
    void anItemTeleportMayAlsoStartFromAFixedTile() {
        TransportDefinition fixed = TransportDefinition.itemTeleport("test:fixed", DESTINATION, 8007)
                .origin(ORIGIN)
                .build();

        assertFalse(fixed.isUsableAnywhere());
        assertEquals(ORIGIN, fixed.getOrigin());
    }

    @Test
    void tilesOutsideTheWorldAreRejected() {
        for (WorldPoint outside : new WorldPoint[]{
                new WorldPoint(-1, 3200, 0),
                new WorldPoint(3200, 16384, 0),
                new WorldPoint(3200, 3200, 4),
                new WorldPoint(3200, 3200, -1)}) {
            assertThrows(IllegalArgumentException.class,
                    () -> TransportDefinition.object("test:door", outside, DESTINATION, 1, "Open").build(),
                    outside.toString());
            assertThrows(IllegalArgumentException.class,
                    () -> TransportDefinition.object("test:door", ORIGIN, outside, 1, "Open").build(),
                    outside.toString());
        }
    }

    @Test
    void aDeclarativeDefinitionMustSayHowItIsOperatedAndWhatToClick() {
        assertThrows(IllegalArgumentException.class, () -> TransportDefinition.builder()
                .id("test:no-shape")
                .origin(ORIGIN)
                .destination(DESTINATION)
                .entityId(1)
                .build());

        assertThrows(IllegalArgumentException.class, () -> TransportDefinition.builder()
                .id("test:no-target")
                .origin(ORIGIN)
                .destination(DESTINATION)
                .shape(TransportShape.SINGLE_CLICK)
                .build());
    }

    @Test
    void aNegativeDurationIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> TransportDefinition.object("test:door", ORIGIN, DESTINATION, 1, "Open").duration(-1).build());
    }

    @Test
    void aCategoryFollowsThePlanningOptionThatCoversIt() {
        GlobalPathfinderConfig noBoats = GlobalPathfinderConfig.builder().useBoats(false).build();

        assertFalse(TransportCategory.BOAT.isEnabled(noBoats));
        assertTrue(TransportCategory.SHIP.isEnabled(noBoats));
        assertTrue(TransportCategory.GENERAL.isEnabled(noBoats));
        assertTrue(TransportCategory.BOAT.isEnabled(null));
    }

    @Test
    void everyOptionCategoryTurnsOffWithItsOwnOption() {
        GlobalPathfinderConfig allOff = GlobalPathfinderConfig.builder()
                .useAgilityShortcuts(false)
                .useGrappleShortcuts(false)
                .useBoats(false)
                .useCanoes(false)
                .useCharterShips(false)
                .useShips(false)
                .useFairyRings(false)
                .useGnomeGliders(false)
                .useHotAirBalloons(false)
                .useMagicCarpets(false)
                .useMagicMushtrees(false)
                .useMinecarts(false)
                .useQuetzals(false)
                .useSeasonalTransports(false)
                .useSpiritTrees(false)
                .useTeleportationItems(false)
                .useTeleportationBoxes(false)
                .useTeleportationLevers(false)
                .useTeleportationMinigames(false)
                .useTeleportationPortals(false)
                .useTeleportationPortalsPoh(false)
                .useTeleportationSpells(false)
                .useWildernessObelisks(false)
                .build();
        GlobalPathfinderConfig allOn = GlobalPathfinderConfig.builder().build();

        for (TransportCategory category : TransportCategory.values()) {
            assertTrue(category.isEnabled(allOn), category + " should be on by default");
            boolean hasNoOption = category == TransportCategory.GENERAL
                    || category == TransportCategory.TELEPORTATION_SPELL_HOME;
            assertEquals(hasNoOption, category.isEnabled(allOff), category + " should follow its planning option");
        }
    }
}
