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

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

    /** The id prefix of the rows {@code scripts/update_walker_transports.py} writes. */
    private static final String GENERATED_ID_PREFIX = "kraken:upstream-";

    private static final String MINIMAL_ROW = "{"
            + "\"id\": \"kraken:door\","
            + "\"origin\": {\"x\": 3200, \"y\": 3200, \"plane\": 0},"
            + "\"destination\": {\"x\": 3200, \"y\": 3201, \"plane\": 0},"
            + "\"shape\": \"SINGLE_CLICK\","
            + "\"entityId\": 1535"
            + "}";

    /** The minimal row spaced the way {@code BundledTransports.toJson} writes a row. */
    private static final String SPACED_ROW = MINIMAL_ROW.replace(",\"", ", \"");

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
            if (shape == TransportShape.WALK) {
                // A step that is only walked is the one shape that must name nothing to click.
                row = row.replace("\"entityId\": 1535, ", "");
            }

            assertEquals(TransportDefinition.DECLARATIVE_SHAPES.contains(shape),
                    !BundledTransports.parse("[" + row + "]").isEmpty(), shape.name() + ": " + row);
        }
    }

    @Test
    void aRowThatStatesNoShapeIsOperatedTheUsualWayForItsCategory() {
        String door = MINIMAL_ROW.replace("\"shape\": \"SINGLE_CLICK\",", "");
        String boat = door.replace("\"entityId\"", "\"category\": \"BOAT\", \"entityId\"");
        String canoe = door.replace("\"entityId\"",
                "\"category\": \"CANOE\", \"displayInfo\": \"Edgeville\", \"entityId\"");
        String glider = door.replace("\"entityId\"", "\"category\": \"GNOME_GLIDER\", \"entityId\"");

        assertEquals(TransportShape.SINGLE_CLICK, BundledTransports.parse("[" + door + "]").get(0).getShape());
        assertEquals(TransportShape.CLICK_THEN_DIALOGUE, BundledTransports.parse("[" + boat + "]").get(0).getShape());
        assertEquals(TransportShape.CANOE, BundledTransports.parse("[" + canoe + "]").get(0).getShape());
        assertTrue(BundledTransports.parse("[" + glider + "]").isEmpty(),
                "the handler for a glider's shape reads more than a row a person writes is trusted to state");
    }

    @Test
    void aCanoeAndAMinigameTeleportCanBeWrittenAsRowsAndReadBack() {
        WorldPoint station = new WorldPoint(3243, 3237, 0);
        WorldPoint edgeville = new WorldPoint(3128, 3503, 0);
        TransportDefinition canoe = TransportDefinition.builder()
                .id("test:canoe")
                .origin(station)
                .destination(edgeville)
                .category(TransportCategory.CANOE)
                .menuTarget("Canoe Station")
                .entityId(12163)
                .displayInfo("Edgeville")
                .duration(30)
                .requirements(DefinitionRequirements.builder().skill(Skill.WOODCUTTING, 42).build())
                .build();
        TransportDefinition teleport = TransportDefinition.builder()
                .id("test:castle-wars")
                .destination(new WorldPoint(2442, 3093, 0))
                .shape(TransportShape.GROUPING_TELEPORT)
                .displayInfo("Castle Wars")
                .duration(24)
                .maxWildernessLevel(0)
                .build();

        TransportDefinition canoeRead = BundledTransports.parse("[" + BundledTransports.toJson(canoe) + "]").get(0);
        TransportDefinition teleportRead = BundledTransports.parse("[" + BundledTransports.toJson(teleport) + "]").get(0);

        assertEquals(TransportShape.CANOE, canoeRead.getShape());
        assertEquals(TransportCategory.CANOE, canoeRead.getCategory());
        assertEquals("Edgeville", canoeRead.getDisplayInfo());
        assertEquals(12163, canoeRead.getEntityId());
        assertEquals(42, canoeRead.getRequirements().getSkillLevels().get(Skill.WOODCUTTING));

        assertEquals(TransportShape.GROUPING_TELEPORT, teleportRead.getShape());
        assertEquals(TransportCategory.TELEPORTATION_MINIGAME, teleportRead.getCategory());
        assertTrue(teleportRead.isUsableAnywhere());
        assertEquals("Castle Wars", teleportRead.getDisplayInfo());
        assertEquals(0, teleportRead.getMaxWildernessLevel());
    }

    @Test
    void aClickCanBeStatedAsOneStringTheWayTheDatasetStatesIt() {
        String raw = SPACED_ROW.replace("\"entityId\": 1535", "\"objectInfo\": \"Pass through Gate 2882\"");
        String both = SPACED_ROW.replace("\"entityId\": 1535", "\"entityId\": 1535, \"objectInfo\": \"Open Door 1535\"");

        TransportDefinition definition = BundledTransports.parse("[" + raw + "]").get(0);

        assertTrue(definition.statesObjectInfo());
        assertEquals("Pass through Gate 2882", definition.getObjectInfo());
        assertEquals(2882, definition.toObjectInfo().getId());
        assertEquals("Gate", definition.toObjectInfo().withEntityName("Gate").getMenuTarget());
        assertEquals("Pass through", definition.toObjectInfo().withEntityName("Gate").getMenuOption());
        assertEquals(raw, BundledTransports.toJson(definition));
        assertTrue(BundledTransports.parse("[" + both + "]").isEmpty(), "a row states its click one way or the other");
    }

    @Test
    void itemRequirementsCanNameAlternativesStavesOffhandsAndAWaiver() {
        String requirements = "\"requirements\": {\"combatLevel\": 70, \"totalLevel\": -1, \"items\": ["
                + "{\"anyOf\": [556, 4696], \"quantity\": 3, \"staves\": [1381], \"offhands\": [20714]}, "
                + "{\"anyOf\": [1854], \"or\": [{\"anyOf\": [995], \"quantity\": 5}]}, "
                + "{\"anyOf\": [2376], \"quantity\": 0}, "
                + "{\"anyOf\": [772], \"waivedByVarbit\": {\"id\": 4498, \"comparison\": \"EQUAL\", \"value\": 1}}], "
                + "\"varPlayers\": [{\"id\": 892, \"comparison\": \"COOLDOWN_MINUTES\", \"value\": 30}, "
                + "{\"id\": 4560, \"comparison\": \"ANY_BIT_OF\", \"value\": 249}]}";
        String row = SPACED_ROW.substring(0, SPACED_ROW.length() - 1) + ", " + requirements + "}";

        TransportDefinition definition = BundledTransports.parse("[" + row + "]").get(0);
        List<DefinitionRequirements.RequiredItem> items = definition.getRequirements().getItems();

        assertEquals(70, definition.getRequirements().getCombatLevel());
        assertEquals(DefinitionRequirements.MAX_TOTAL_LEVEL, definition.getRequirements().getTotalLevel());
        assertEquals(3, items.get(0).getQuantity());
        assertEquals(1381, items.get(0).getStaves()[0]);
        assertEquals(20714, items.get(0).getOffhands()[0]);
        assertEquals(5, items.get(1).getAlternatives().get(0).getQuantity());
        assertEquals(0, items.get(2).getQuantity(), "a quantity of zero is kept: the item must not be held");
        assertEquals(1, items.get(3).getQuantity(), "a quantity that is left out is one");
        assertEquals(4498, items.get(3).getWaivedByVarbit().getId());
        assertEquals(DefinitionRequirements.Comparison.COOLDOWN_MINUTES,
                definition.getRequirements().getVarPlayers().get(0).getComparison());
        assertEquals(row, BundledTransports.toJson(definition), "the row is written back as it was read");

        String misspelled = row.replace("\"staves\"", "\"staffs\"");
        String nested = row.replace("{\"anyOf\": [995], \"quantity\": 5}", "{\"anyOf\": [995], \"or\": []}");
        assertTrue(BundledTransports.parse("[" + misspelled + "]").isEmpty());
        assertTrue(BundledTransports.parse("[" + nested + "]").isEmpty(), "an alternative carries no alternatives");
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
    void everyBundledRowBelongsToKraken() {
        for (TransportDefinition definition : BundledTransports.load()) {
            assertEquals("kraken", definition.getOwner(), definition.getId());
        }
    }

    @Test
    void theBundledFileHoldsOnlyRowsWrittenByHand() {
        for (TransportDefinition definition : BundledTransports.load()) {
            assertFalse(definition.getId().startsWith(GENERATED_ID_PREFIX),
                    definition.getId() + " is generated and belongs in the dataset");
            assertTrue(definition.isReplaces(), definition.getId() + " is there to correct a dataset transport");
        }
    }

    @Test
    void aWrittenRowReadsBackAsTheSameDefinition() {
        TransportDefinition original = BundledTransports.parse("[" + FULL_ROW + "]").get(0);

        String written = BundledTransports.toJson(original);
        TransportDefinition readBack = BundledTransports.parse("[" + written + "]").get(0);

        assertEquals(original.getId(), readBack.getId());
        assertEquals(original.getOrigin(), readBack.getOrigin());
        assertEquals(original.getDestination(), readBack.getDestination());
        assertEquals(original.getCategory(), readBack.getCategory());
        assertEquals(original.getShape(), readBack.getShape());
        assertEquals(original.getObjectInfo(), readBack.getObjectInfo());
        assertEquals(original.getDisplayInfo(), readBack.getDisplayInfo());
        assertEquals(original.getDuration(), readBack.getDuration());
        assertEquals(original.isBidirectional(), readBack.isBidirectional());
        assertEquals(original.isReplaces(), readBack.isReplaces());
        assertEquals(original.getRequirements().getSkillLevels(), readBack.getRequirements().getSkillLevels());
        assertEquals(original.getRequirements().getQuests(), readBack.getRequirements().getQuests());
        assertEquals(original.getRequirements().getItems(), readBack.getRequirements().getItems());
        assertEquals(original.getRequirements().getVarbits(), readBack.getRequirements().getVarbits());
        assertEquals(original.getRequirements().getVarPlayers(), readBack.getRequirements().getVarPlayers());
        assertEquals(original.getRequirements().isMembersOnly(), readBack.getRequirements().isMembersOnly());
        assertEquals(written, BundledTransports.toJson(readBack), "writing is stable over a round trip");
    }

    @Test
    void aWrittenRowLeavesOutWhatTheLoaderFillsIn() {
        TransportDefinition door = BundledTransports.parse("[" + MINIMAL_ROW + "]").get(0);

        assertEquals("{\"id\": \"kraken:door\", \"origin\": {\"x\": 3200, \"y\": 3200, \"plane\": 0}, "
                + "\"destination\": {\"x\": 3200, \"y\": 3201, \"plane\": 0}, \"shape\": \"SINGLE_CLICK\", "
                + "\"entityId\": 1535}", BundledTransports.toJson(door));
    }

    @Test
    void anItemTeleportIsWrittenWithoutTheHoldingRuleItsItemIdsImply() {
        TransportDefinition glory = TransportDefinition
                .itemTeleport("kraken:glory-edgeville", new WorldPoint(3087, 3496, 0), 1712, 1710)
                .menuOption("Rub")
                .displayInfo("Edgeville")
                .maxWildernessLevel(30)
                .requirements(DefinitionRequirements.builder().quest(Quest.DRAGON_SLAYER_I).build())
                .build();

        String written = BundledTransports.toJson(glory);
        TransportDefinition readBack = BundledTransports.parse("[" + written + "]").get(0);

        assertEquals("{\"id\": \"kraken:glory-edgeville\", \"destination\": {\"x\": 3087, \"y\": 3496, \"plane\": 0}, "
                + "\"shape\": \"ITEM_SUBOP\", \"menuOption\": \"Rub\", \"itemIds\": [1712, 1710], "
                + "\"displayInfo\": \"Edgeville\", \"maxWildernessLevel\": 30, "
                + "\"requirements\": {\"quests\": [\"DRAGON_SLAYER_I\"]}}", written);
        assertEquals(1, readBack.getRequirements().getItems().size(), "the holding rule is added once, on load");
    }

    @Test
    void aNameIsWrittenAsPlainAsciiAndReadBackUnchanged() {
        TransportDefinition odd = TransportDefinition
                .npc("kraken:odd", new WorldPoint(3200, 3200, 0), new WorldPoint(3300, 3300, 0),
                        "Café \"owner\" \\ Zoë", "Talk-to")
                .build();

        String written = BundledTransports.toJson(odd);

        assertTrue(written.chars().allMatch(c -> c >= 0x20 && c <= 0x7e), written);
        assertEquals("Café \"owner\" \\ Zoë",
                BundledTransports.parse("[" + written + "]").get(0).getMenuTarget());
    }

    @Test
    void aDefinitionThatAFileCannotHoldIsNotWritten() {
        WorldPoint origin = new WorldPoint(3200, 3200, 0);
        WorldPoint destination = new WorldPoint(3300, 3300, 0);
        TransportDefinition withHandler = TransportDefinition.object("test:handler", origin, destination, 1, "Open")
                .handler(context -> true)
                .build();
        TransportDefinition withCondition = TransportDefinition.object("test:condition", origin, destination, 1, "Open")
                .availableWhen(() -> true)
                .build();
        TransportDefinition needsHandler = TransportDefinition.object("test:glider", origin, destination, 1, "Glider")
                .category(TransportCategory.GNOME_GLIDER)
                .shape(TransportShape.CLICK_THEN_WIDGET)
                .build();

        assertThrows(IllegalArgumentException.class, () -> BundledTransports.toJson(withHandler));
        assertThrows(IllegalArgumentException.class, () -> BundledTransports.toJson(withCondition));
        assertThrows(IllegalArgumentException.class, () -> BundledTransports.toJson(needsHandler));
        assertThrows(IllegalArgumentException.class, () -> BundledTransports.toJson(null));
    }

    @Test
    void everyRowOfTheBundledFileIsWrittenBackAsTheFileHasIt() throws Exception {
        List<String> lines;
        try (var stream = BundledTransports.class.getResourceAsStream(BundledTransports.RESOURCE_PATH)) {
            lines = new String(stream.readAllBytes(), StandardCharsets.UTF_8).lines()
                    .map(String::trim)
                    .filter(line -> line.startsWith("{"))
                    .map(line -> line.endsWith(",") ? line.substring(0, line.length() - 1) : line)
                    .collect(Collectors.toList());
        }
        List<TransportDefinition> definitions = BundledTransports.load();

        assertEquals(lines.size(), definitions.size());
        for (int i = 0; i < lines.size(); i++) {
            assertEquals(lines.get(i), BundledTransports.toJson(definitions.get(i)));
        }
    }

    private static String withRequirements(String requirements) {
        return MINIMAL_ROW.substring(0, MINIMAL_ROW.length() - 1) + ", \"requirements\": " + requirements + "}";
    }
}
