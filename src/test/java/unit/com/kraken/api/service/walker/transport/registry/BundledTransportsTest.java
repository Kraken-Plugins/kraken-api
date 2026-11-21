package unit.com.kraken.api.service.walker.transport.registry;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonParseException;
import com.kraken.api.service.walker.transport.TransportShape;
import com.kraken.api.service.walker.transport.registry.BundledTransports;
import com.kraken.api.service.walker.transport.registry.DefinitionRequirements;
import com.kraken.api.service.walker.transport.registry.DefinitionRequirements.Comparison;
import com.kraken.api.service.walker.transport.registry.TransportCategory;
import com.kraken.api.service.walker.transport.registry.TransportDefinition;
import com.kraken.api.util.JsonResourceUtils;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the transports that ship inside the jar: the file format, and the promise that one bad row
 * costs only that row.
 */
class BundledTransportsTest {

    private static final String FULL_ROW = "{"
            + "\"id\": \"kraken:temple-entrance\","
            + "\"origin\": {\"x\": 1312, \"y\": 3685, \"plane\": 0},"
            + "\"destination\": {\"x\": 1312, \"y\": 10086, \"plane\": 0},"
            + "\"category\": \"AGILITY_SHORTCUT\","
            + "\"shape\": \"CLICK_THEN_DIALOGUE\","
            + "\"menuOption\": \"Enter\","
            + "\"menuTarget\": \"Lizard dwelling\","
            + "\"entityId\": 34405,"
            + "\"displayInfo\": \"Yes\","
            + "\"duration\": 3,"
            + "\"bidirectional\": true,"
            + "\"replaces\": true,"
            + "\"requirements\": {"
            + "  \"membersOnly\": true,"
            + "  \"skills\": {\"AGILITY\": 50},"
            + "  \"quests\": [\"CHILDREN_OF_THE_SUN\"],"
            + "  \"items\": [{\"anyOf\": [995], \"quantity\": 10}, {\"anyOf\": [954, 955]}],"
            + "  \"varbits\": [{\"id\": 100, \"comparison\": \"AT_LEAST\", \"value\": 7}],"
            + "  \"varPlayers\": [{\"id\": 273, \"comparison\": \"EQUAL\", \"value\": 1}]"
            + "}}";

    private static final String MINIMAL_ROW = "{"
            + "\"id\": \"kraken:door\","
            + "\"origin\": {\"x\": 3200, \"y\": 3200, \"plane\": 0},"
            + "\"destination\": {\"x\": 3200, \"y\": 3201, \"plane\": 0},"
            + "\"shape\": \"SINGLE_CLICK\","
            + "\"entityId\": 1535"
            + "}";

    @Test
    void everyFieldOfARowReachesTheDefinition() {
        TransportDefinition definition = BundledTransports.parse("[" + FULL_ROW + "]").get(0);

        assertEquals("kraken:temple-entrance", definition.getId());
        assertEquals(new WorldPoint(1312, 3685, 0), definition.getOrigin());
        assertEquals(new WorldPoint(1312, 10086, 0), definition.getDestination());
        assertEquals(TransportCategory.AGILITY_SHORTCUT, definition.getCategory());
        assertEquals(TransportShape.CLICK_THEN_DIALOGUE, definition.getShape());
        assertEquals("Enter Lizard dwelling 34405", definition.getObjectInfo());
        assertEquals("Yes", definition.getDisplayInfo());
        assertEquals(3, definition.getDuration());
        assertTrue(definition.isBidirectional());
        assertTrue(definition.isReplaces());

        DefinitionRequirements requirements = definition.getRequirements();
        assertTrue(requirements.isMembersOnly());
        assertEquals(50, requirements.getSkillLevels().get(Skill.AGILITY));
        assertEquals(Set.of(Quest.CHILDREN_OF_THE_SUN), requirements.getQuests());
        assertArrayEquals(new int[]{995}, requirements.getItems().get(0).getAnyOf());
        assertEquals(10, requirements.getItems().get(0).getQuantity());
        assertArrayEquals(new int[]{954, 955}, requirements.getItems().get(1).getAnyOf());
        assertEquals(1, requirements.getItems().get(1).getQuantity(), "a missing quantity means one");
        assertEquals(Comparison.AT_LEAST, requirements.getVarbits().get(0).getComparison());
        assertEquals(7, requirements.getVarbits().get(0).getValue());
        assertEquals(273, requirements.getVarPlayers().get(0).getId());
    }

    @Test
    void aRowNeedsOnlyItsIdTilesShapeAndClickTarget() {
        TransportDefinition definition = BundledTransports.parse("[" + MINIMAL_ROW + "]").get(0);

        assertEquals(TransportCategory.GENERAL, definition.getCategory());
        assertEquals(1, definition.getDuration());
        assertSame(DefinitionRequirements.NONE, definition.getRequirements());
        assertFalse(definition.isBidirectional());
    }

    @Test
    void aRowThatIsMissingSomethingRequiredIsSkippedAndTheRestLoad() {
        String noDestination = "{\"id\": \"kraken:broken\", \"origin\": {\"x\": 1, \"y\": 1, \"plane\": 0},"
                + " \"shape\": \"SINGLE_CLICK\", \"entityId\": 1}";
        String unknownShape = MINIMAL_ROW.replace("SINGLE_CLICK", "TELEKINESIS");
        String wrongType = MINIMAL_ROW.replace("1535", "\"not a number\"");

        List<TransportDefinition> definitions = BundledTransports.parse(
                "[" + noDestination + "," + unknownShape + "," + wrongType + ", null," + MINIMAL_ROW + "]");

        assertEquals(1, definitions.size());
        assertEquals("kraken:door", definitions.get(0).getId());
    }

    @Test
    void aRequirementValueThatDoesNotResolveSkipsTheRowRatherThanDroppingTheRequirement() {
        String unknownQuest = withRequirements("{\"quests\": [\"NOT_A_QUEST\"]}");
        String unknownSkill = withRequirements("{\"skills\": {\"JUGGLING\": 50}}");
        String unknownComparison = withRequirements("{\"varbits\": [{\"id\": 1, \"comparison\": \"ROUGHLY\", \"value\": 1}]}");
        String noItemIds = withRequirements("{\"items\": [{\"quantity\": 10}]}");

        assertTrue(BundledTransports.parse("[" + unknownQuest + "]").isEmpty());
        assertTrue(BundledTransports.parse("[" + unknownSkill + "]").isEmpty());
        assertTrue(BundledTransports.parse("[" + unknownComparison + "]").isEmpty());
        assertTrue(BundledTransports.parse("[" + noItemIds + "]").isEmpty());
    }

    @Test
    void aMisspelledKeySkipsTheRowRatherThanLoadingItWithoutThatField() {
        String misspelledRequirements = MINIMAL_ROW.replace("\"shape\"", "\"requirments\": {}, \"shape\"");
        String misspelledSkills = withRequirements("{\"skils\": {\"AGILITY\": 50}}");
        String misspelledMembers = withRequirements("{\"membersonly\": true}");
        String misspelledItemKey = withRequirements("{\"items\": [{\"anyOf\": [995], \"amount\": 10}]}");
        String misspelledVarKey = withRequirements("{\"varbits\": [{\"id\": 1, \"comparison\": \"EQUAL\", \"val\": 1}]}");
        String misspelledPointKey = MINIMAL_ROW.replace("\"plane\": 0},\"destination\"", "\"z\": 0},\"destination\"");

        for (String row : List.of(misspelledRequirements, misspelledSkills, misspelledMembers,
                misspelledItemKey, misspelledVarKey, misspelledPointKey)) {
            assertTrue(BundledTransports.parse("[" + row + "]").isEmpty(), row);
        }
    }

    @Test
    void aCategoryThatIsNotAKnownNameSkipsTheRowRatherThanBecomingGeneral() {
        String withCategory = MINIMAL_ROW.replace("\"shape\"", "\"category\": \"BOAT\", \"shape\"");

        assertEquals(TransportCategory.BOAT, BundledTransports.parse("[" + withCategory + "]").get(0).getCategory());
        assertTrue(BundledTransports.parse("[" + withCategory.replace("BOAT", "BOATS") + "]").isEmpty());
        assertTrue(BundledTransports.parse("[" + withCategory.replace("BOAT", "boat") + "]").isEmpty());
    }

    @Test
    void aShapeThatNeedsAHandlerCannotComeFromAFile() {
        // The row carries what every declarative shape works from: a click target, an item and a
        // spell name. Only the shape decides whether it loads.
        String complete = MINIMAL_ROW.replace("\"entityId\": 1535",
                "\"entityId\": 1535, \"itemIds\": [8007], \"displayInfo\": \"Varrock Teleport\"");

        for (TransportShape shape : TransportShape.values()) {
            String row = complete.replace("SINGLE_CLICK", shape.name());

            assertEquals(TransportDefinition.DECLARATIVE_SHAPES.contains(shape),
                    !BundledTransports.parse("[" + row + "]").isEmpty(), shape.name());
        }
    }

    @Test
    void aRowWithoutAnOriginIsAnItemTeleportUsableFromAnywhere() {
        String row = "{"
                + "\"id\": \"kraken:glory-edgeville\","
                + "\"destination\": {\"x\": 3087, \"y\": 3496, \"plane\": 0},"
                + "\"shape\": \"ITEM_SUBOP\","
                + "\"itemIds\": [1712, 1710, 1708, 1706],"
                + "\"menuOption\": \"Rub\","
                + "\"displayInfo\": \"Edgeville\","
                + "\"maxWildernessLevel\": 30,"
                + "\"duration\": 4"
                + "}";

        TransportDefinition definition = BundledTransports.parse("[" + row + "]").get(0);

        assertTrue(definition.isUsableAnywhere());
        assertEquals(new WorldPoint(3087, 3496, 0), definition.getDestination());
        assertEquals(TransportCategory.TELEPORTATION_ITEM, definition.getCategory());
        assertEquals(List.of(1712, 1710, 1708, 1706), definition.getItemIds());
        assertEquals("Rub", definition.getMenuOption());
        assertEquals("Edgeville", definition.getDisplayInfo());
        assertEquals(30, definition.getMaxWildernessLevel());
        assertArrayEquals(new int[]{1712, 1710, 1708, 1706},
                definition.getRequirements().getItems().get(0).getAnyOf());
    }

    @Test
    void aRowWithoutAnOriginCanBeASpell() {
        String row = "{"
                + "\"id\": \"kraken:varrock-teleport\","
                + "\"destination\": {\"x\": 3213, \"y\": 3424, \"plane\": 0},"
                + "\"shape\": \"SPELL\","
                + "\"displayInfo\": \"Varrock Teleport\""
                + "}";

        TransportDefinition definition = BundledTransports.parse("[" + row + "]").get(0);

        assertTrue(definition.isUsableAnywhere());
        assertEquals(TransportCategory.TELEPORTATION_SPELL, definition.getCategory());
        assertEquals(TransportDefinition.DEFAULT_MAX_WILDERNESS_LEVEL, definition.getMaxWildernessLevel());
    }

    @Test
    void aTeleportRowThatLeavesOutWhatItsShapeNeedsIsSkipped() {
        String noItems = "{\"id\": \"kraken:tab\", \"destination\": {\"x\": 3213, \"y\": 3424, \"plane\": 0},"
                + " \"shape\": \"ITEM_SUBOP\"}";
        String noSpell = noItems.replace("ITEM_SUBOP", "SPELL");
        String doorWithoutOrigin = MINIMAL_ROW.replace(
                "\"origin\": {\"x\": 3200, \"y\": 3200, \"plane\": 0},", "");
        String misspelledItems = noItems.replace("\"shape\"", "\"itemIDs\": [8007], \"shape\"");

        for (String row : List.of(noItems, noSpell, doorWithoutOrigin, misspelledItems)) {
            assertTrue(BundledTransports.parse("[" + row + "]").isEmpty(), row);
        }
    }

    @Test
    void textThatIsNotAnArrayOfRowsIsRejected() {
        assertThrows(JsonParseException.class, () -> BundledTransports.parse("{\"id\": \"kraken:door\"}"));
        assertTrue(BundledTransports.parse("[]").isEmpty());
    }

    @Test
    void everyRowOfTheBundledFileLoadsUnderItsOwnId() {
        JsonArray rows = JsonResourceUtils.loadJsonResource(
                BundledTransports.class, BundledTransports.RESOURCE_PATH, new Gson(), JsonArray.class);

        List<TransportDefinition> definitions = BundledTransports.load();

        assertEquals(rows.size(), definitions.size(), "a row in " + BundledTransports.RESOURCE_PATH + " was skipped");

        Set<String> ids = new HashSet<>();
        for (TransportDefinition definition : definitions) {
            assertTrue(ids.add(definition.getId()), "duplicate id " + definition.getId());
        }
    }

    @Test
    void theBundledFileHoldsOnlyTransportsThatOneClickOperates() {
        List<TransportDefinition> definitions = BundledTransports.load();

        assertFalse(definitions.isEmpty(), "the bundled file ships with transports");
        for (TransportDefinition definition : definitions) {
            String id = definition.getId();
            assertEquals("kraken", definition.getOwner(), id);
            assertFalse(definition.isReplaces(), id + " must not hide a dataset transport");

            if (definition.isUsableAnywhere()) {
                assertEquals(TransportShape.ITEM_SUBOP, definition.getShape(), id);
                assertEquals(TransportCategory.TELEPORTATION_ITEM, definition.getCategory(), id);
                assertFalse(definition.getItemIds().isEmpty(), id);
            } else {
                assertEquals(TransportShape.SINGLE_CLICK, definition.getShape(), id);
                assertTrue(definition.toObjectInfo().hasId(), id + " names no object id");
                assertFalse(definition.toObjectInfo().getMenuOption().isEmpty(), id + " names no menu option");
                assertFalse(definition.toObjectInfo().getMenuTarget().isEmpty(), id + " names no object");
            }
        }
    }

    private static String withRequirements(String requirements) {
        return MINIMAL_ROW.substring(0, MINIMAL_ROW.length() - 1) + ", \"requirements\": " + requirements + "}";
    }
}
