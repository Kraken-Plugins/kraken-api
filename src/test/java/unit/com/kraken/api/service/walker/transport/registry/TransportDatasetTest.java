package com.kraken.api.service.walker.transport.registry;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.kraken.api.service.walker.transport.GroupingTeleport;
import com.kraken.api.service.walker.transport.TransportShape;
import com.kraken.api.service.walker.transport.handler.SpellTeleportHandler;
import net.runelite.api.coords.WorldPoint;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the transport dataset that ships with the API: that every row of the generated file
 * loads, that hub networks are paired up the way their stops say, and that the rows carry what the
 * walker needs to operate them.
 */
class TransportDatasetTest {

    private static String json;
    private static TransportDataset.Result shipped;

    @BeforeAll
    static void load() throws Exception {
        try (InputStream stream = TransportDataset.class.getResourceAsStream(TransportDataset.RESOURCE_PATH)) {
            assertNotNull(stream, "the dataset is missing from the classpath");
            json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
        shipped = TransportDataset.parse(json);
    }

    private static Map<TransportCategory, Long> countByCategory() {
        Map<TransportCategory, Long> counts = new EnumMap<>(TransportCategory.class);
        for (TransportDefinition definition : shipped.getDefinitions()) {
            counts.merge(definition.getCategory(), 1L, Long::sum);
        }
        return counts;
    }

    @Test
    void everyRowAndEveryStopOfTheShippedFileLoads() {
        assertEquals(List.of(), shipped.getProblems());

        JsonObject root = new Gson().fromJson(json, JsonObject.class);
        int expected = root.getAsJsonArray("transports").size();
        assertTrue(expected > 9_000, "the file holds " + expected + " rows");
        Set<String> rowIds = new HashSet<>();
        for (JsonElement row : root.getAsJsonArray("transports")) {
            rowIds.add(row.getAsJsonObject().get("id").getAsString());
        }
        long fromRows = shipped.getDefinitions().stream()
                .filter(definition -> rowIds.contains(definition.getId()))
                .count();
        assertEquals(expected, rowIds.size(), "no two rows share an id");
        assertEquals(expected, fromRows, "one transport per row");
    }

    @Test
    void theBundledDatasetIsTheShippedFileAndIsReadOnce() {
        assertEquals(shipped.getDefinitions().size(), TransportDataset.bundled().size());
        assertSame(TransportDataset.bundled(), TransportDataset.bundled());
    }

    @Test
    void noTwoTransportsShareAnId() {
        Set<String> ids = new HashSet<>();
        for (TransportDefinition definition : shipped.getDefinitions()) {
            assertTrue(ids.add(definition.getId()), definition.getId() + " appears twice");
            assertTrue(definition.getId().startsWith("kraken:upstream-"), definition.getId());
        }
    }

    @Test
    void everyKindOfTransportUpstreamListsIsThere() {
        Map<TransportCategory, Long> counts = countByCategory();

        for (TransportCategory category : TransportCategory.values()) {
            assertTrue(counts.getOrDefault(category, 0L) > 0, category + " has no transport in the dataset");
        }
        assertTrue(counts.get(TransportCategory.GENERAL) > 6_000, counts.toString());
        assertTrue(counts.get(TransportCategory.AGILITY_SHORTCUT) > 600, counts.toString());
        assertTrue(counts.get(TransportCategory.TELEPORTATION_ITEM) > 400, counts.toString());
        assertTrue(counts.get(TransportCategory.CHARTER_SHIP) > 300, counts.toString());
    }

    @Test
    void aHubIsStoredAsStopsAndPlannedAsEveryPairOfThem() {
        JsonObject root = new Gson().fromJson(json, JsonObject.class);
        JsonObject fairyRings = null;
        for (JsonElement hub : root.getAsJsonArray("hubs")) {
            if (hub.getAsJsonObject().get("id").getAsString().equals("kraken:upstream-fairy-rings")) {
                fairyRings = hub.getAsJsonObject();
            }
        }
        assertNotNull(fairyRings);
        int origins = fairyRings.getAsJsonArray("origins").size();
        int destinations = fairyRings.getAsJsonArray("destinations").size();
        long pairs = countByCategory().get(TransportCategory.FAIRY_RING);

        assertTrue(origins > 50 && destinations > 50, origins + " origins, " + destinations + " destinations");
        assertTrue(pairs > (long) origins * (destinations - 2), pairs + " pairs from " + origins + " rings");
        assertTrue(pairs < (long) origins * destinations, "a ring is not paired with itself");
    }

    @Test
    void aHubPairIsClickedAtItsOriginAndChoosesItsDestination() {
        TransportDefinition ring = shipped.getDefinitions().stream()
                .filter(definition -> definition.getCategory() == TransportCategory.FAIRY_RING)
                .findFirst()
                .orElseThrow();

        assertEquals(TransportShape.FAIRY_RING, ring.getShape());
        assertNotNull(ring.toObjectInfo(), ring.getId() + " names no ring to click");
        assertNotNull(ring.getDisplayInfo(), ring.getId() + " names no code to dial");
        assertFalse(ring.getRequirements().getItems().isEmpty(), "a fairy ring needs a staff");
        assertFalse(ring.getRequirements().getVarbits().isEmpty(), "a fairy ring needs Fairytale II under way");
        assertTrue(ring.getOrigin().distanceTo2D(ring.getDestination()) > 6);
    }

    @Test
    void aHubSharesItsRequirementsBetweenThePairsThatAddNothing() {
        List<TransportDefinition> rings = shipped.getDefinitions().stream()
                .filter(definition -> definition.getCategory() == TransportCategory.FAIRY_RING)
                .collect(Collectors.toList());
        long distinct = rings.stream().map(TransportDefinition::getRequirements)
                .collect(Collectors.toCollection(() -> java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>())))
                .size();

        assertTrue(distinct < rings.size() / 10, distinct + " requirement objects for " + rings.size() + " rings");
    }

    @Test
    void everyTransportCanBeOperatedFromWhatItStates() {
        for (TransportDefinition definition : shipped.getDefinitions()) {
            String id = definition.getId();
            TransportShape shape = definition.getShape();
            assertNotNull(shape, id);

            if (definition.isUsableAnywhere()) {
                assertTrue(TransportDefinition.ORIGIN_FREE_SHAPES.contains(shape), id + " has shape " + shape);
            } else if (shape == TransportShape.WALK) {
                assertEquals(null, definition.toObjectInfo(), id);
            } else {
                assertNotNull(definition.toObjectInfo(), id + " names nothing to click");
            }
            if (shape == TransportShape.ITEM_SUBOP) {
                assertFalse(definition.getItemIds().isEmpty(), id);
            }
            if (shape == TransportShape.CANOE) {
                assertFalse(definition.getDisplayInfo().trim().isEmpty(), id + " names no stop to paddle to");
            }
            if (shape == TransportShape.GROUPING_TELEPORT) {
                assertNotNull(GroupingTeleport.parse(definition.getDisplayInfo()), id + " names no minigame");
            }
            assertTrue(definition.getDuration() >= 0, id);
        }
    }

    @Test
    void aRowKeepsTheClickUpstreamWroteOrTheOneTheCacheConfirmed() {
        long asUpstream = shipped.getDefinitions().stream().filter(TransportDefinition::statesObjectInfo).count();
        long confirmed = shipped.getDefinitions().stream()
                .filter(definition -> !definition.statesObjectInfo() && definition.getEntityId() >= 0)
                .count();

        assertTrue(asUpstream > 0 && confirmed > 0, asUpstream + " as upstream, " + confirmed + " confirmed");
        for (TransportDefinition definition : shipped.getDefinitions()) {
            if (definition.statesObjectInfo()) {
                assertEquals(definition.getObjectInfo(), definition.getObjectInfo().trim());
                assertFalse(definition.getObjectInfo().isEmpty(), definition.getId());
            }
        }
    }

    @Test
    void theTransportsOfAPlayerOwnedHouseMeetOnOneLandingTile() {
        WorldPoint landing = new WorldPoint(1858, 7051, 0);
        for (TransportDefinition definition : shipped.getDefinitions()) {
            for (WorldPoint tile : new WorldPoint[]{definition.getOrigin(), definition.getDestination()}) {
                if (tile != null && tile.getX() >= 1856 && tile.getX() <= 2047 && tile.getY() >= 7040 && tile.getY() <= 7111) {
                    assertEquals(landing, tile, definition.getId());
                }
            }
        }
        assertTrue(shipped.getDefinitions().stream().anyMatch(definition -> landing.equals(definition.getOrigin())));
        assertTrue(shipped.getDefinitions().stream().anyMatch(definition -> landing.equals(definition.getDestination())));
    }

    @Test
    void mostSpellRowsNameASpellTheWalkerCanCast() {
        List<String> unknown = shipped.getDefinitions().stream()
                .filter(definition -> definition.getShape() == TransportShape.SPELL)
                .map(TransportDefinition::getDisplayInfo)
                .distinct()
                .filter(name -> SpellTeleportHandler.findSpell(name) == null)
                .collect(Collectors.toList());
        long spells = shipped.getDefinitions().stream()
                .filter(definition -> definition.getShape() == TransportShape.SPELL)
                .map(TransportDefinition::getDisplayInfo)
                .distinct()
                .count();

        // A spell no spellbook enum names is never planned, since the client cannot be asked
        // whether it is castable. This keeps the number of such rows from growing unnoticed.
        assertTrue(unknown.size() * 4 <= spells, unknown.size() + " of " + spells + " spells are not known: " + unknown);
    }

    @Test
    void aHubWithAStopThatCannotBeReadSaysSoAndKeepsTheRest() {
        String hub = "{\"hubs\": [{\"id\": \"test:gliders\", \"category\": \"GNOME_GLIDER\", \"minDistance\": 6,"
                + " \"origins\": [{\"origin\": {\"x\": 2465, \"y\": 3501, \"plane\": 3}, \"objectInfo\": \"Glider Captain Errdo 10467\"},"
                + " {\"origin\": {\"x\": 2970, \"y\": 2972, \"plane\": 0}, \"objectInfo\": \"Glider Captain Klemfoodle 10479\", \"colour\": \"red\"}],"
                + " \"destinations\": [{\"destination\": {\"x\": 2971, \"y\": 2969, \"plane\": 0}, \"displayInfo\": \"Gandius\", \"duration\": 3},"
                + " {\"destination\": {\"x\": 2464, \"y\": 3501, \"plane\": 3}, \"displayInfo\": \"Ta Quir Priw\"}]}]}";

        TransportDataset.Result result = TransportDataset.parse(hub);

        assertEquals(1, result.getProblems().size(), result.getProblems().toString());
        assertTrue(result.getProblems().get(0).contains("colour"), result.getProblems().toString());
        assertEquals(1, result.getDefinitions().size(), "the stop beside the first origin is too close to pair with it");
        TransportDefinition glider = result.getDefinitions().get(0);
        assertEquals("test:gliders-2465-3501-3-to-2971-2969-0", glider.getId());
        assertEquals(TransportCategory.GNOME_GLIDER, glider.getCategory());
        assertEquals(TransportShape.CLICK_THEN_WIDGET, glider.getShape());
        assertEquals("Glider Captain Errdo 10467", glider.getObjectInfo());
        assertEquals("Gandius", glider.getDisplayInfo());
        assertEquals(3, glider.getDuration());
    }

    @Test
    void aRowWithAnUnknownKeyOrCategoryIsReportedAndSkipped() {
        String rows = "{\"transports\": ["
                + "{\"id\": \"test:good\", \"origin\": {\"x\": 3200, \"y\": 3200, \"plane\": 0},"
                + " \"destination\": {\"x\": 3200, \"y\": 3200, \"plane\": 1}, \"objectInfo\": \"Climb-up Ladder 16683\"},"
                + "{\"id\": \"test:typo\", \"origin\": {\"x\": 3200, \"y\": 3200, \"plane\": 0},"
                + " \"destination\": {\"x\": 3200, \"y\": 3200, \"plane\": 1}, \"objectInfo\": \"Climb-up Ladder 16683\", \"requirments\": {}},"
                + "{\"id\": \"test:kind\", \"destination\": {\"x\": 3200, \"y\": 3200, \"plane\": 1}, \"category\": \"TELEPORTATION_CANNON\"}"
                + "]}";

        TransportDataset.Result result = TransportDataset.parse(rows);

        assertEquals(List.of("test:good"),
                result.getDefinitions().stream().map(TransportDefinition::getId).collect(Collectors.toList()));
        assertEquals(2, result.getProblems().size(), result.getProblems().toString());
    }

    @Test
    void aDatasetRowMayHaveTheShapeAFileAPersonWritesCannot() {
        String glider = "{\"transports\": [{\"id\": \"test:glider\", \"origin\": {\"x\": 2465, \"y\": 3501, \"plane\": 3},"
                + " \"destination\": {\"x\": 2848, \"y\": 3497, \"plane\": 0}, \"category\": \"GNOME_GLIDER\","
                + " \"objectInfo\": \"Glider Captain Errdo 10467\", \"displayInfo\": \"Sindarpos\"}]}";

        TransportDataset.Result result = TransportDataset.parse(glider);

        assertEquals(List.of(), result.getProblems());
        assertEquals(TransportShape.CLICK_THEN_WIDGET, result.getDefinitions().get(0).getShape());
        assertTrue(BundledTransports.parse(glider.substring(glider.indexOf('['), glider.lastIndexOf(']') + 1)).isEmpty());
    }
}
