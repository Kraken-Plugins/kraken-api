package com.kraken.api.service.walker.transport.registry;

import com.kraken.api.service.pathfinding.GlobalPathfinderConfig;
import com.kraken.api.service.walker.transport.TransportRequirements;
import com.kraken.api.service.walker.transport.TransportShape;
import net.runelite.api.Quest;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarPlayerID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the bundled rows that are written by hand to correct single transports of the dataset: the
 * Al Kharid toll gate, which the dataset lists as a free Open, and the webs, which are slashed and
 * then walked through.
 *
 * <p>Each of these rows takes a dataset transport over with {@code replaces}. The tests read the
 * bundled dataset, so a regeneration that moves, removes or corrects one of those transports fails
 * here, where the row can be updated or deleted.</p>
 */
class HandWrittenTransportsTest {

    private static final String GATE_PREFIX = "kraken:al-kharid-gate-";
    private static final String WEB_PREFIX = "kraken:web-";
    private static final WorldPoint SOUTH_WEST = new WorldPoint(3267, 3227, 0);
    private static final WorldPoint SOUTH_EAST = new WorldPoint(3268, 3227, 0);
    private static final WorldPoint NORTH_WEST = new WorldPoint(3267, 3228, 0);
    private static final WorldPoint NORTH_EAST = new WorldPoint(3268, 3228, 0);
    private static final GlobalPathfinderConfig DEFAULTS = GlobalPathfinderConfig.builder().build();

    private static List<TransportDefinition> gates;
    private static List<TransportDefinition> webs;
    private static List<TransportDefinition> dataset;

    @BeforeAll
    static void load() {
        List<TransportDefinition> bundled = BundledTransports.load();
        gates = bundled.stream().filter(row -> row.getId().startsWith(GATE_PREFIX)).collect(Collectors.toList());
        webs = bundled.stream().filter(row -> row.getId().startsWith(WEB_PREFIX)).collect(Collectors.toList());
        dataset = TransportDataset.bundled();
    }

    @Test
    void everyHandWrittenRowIsAGateOrAWeb() {
        assertEquals(gates.size() + webs.size(), BundledTransports.load().size(),
                "a new kind of hand written row needs its own checks here");
    }

    @Test
    void theGateRowsCoverBothGatesInBothDirectionsOnTheLiveObjects() {
        assertEquals(6, gates.size());
        for (TransportDefinition gate : gates) {
            String id = gate.getId();
            boolean south = id.contains("-south-");

            assertEquals(south ? SOUTH_WEST : NORTH_WEST, gate.getOrigin(), id);
            assertEquals(south ? SOUTH_EAST : NORTH_EAST, gate.getDestination(), id);
            assertEquals(south ? 44598 : 44599, gate.getEntityId(), id);
            assertEquals(TransportShape.SINGLE_CLICK, gate.getShape(), id);
            assertTrue(gate.isBidirectional(), id);
            assertTrue(gate.isReplaces(), id + " has to hide the dataset's free gate");
            assertEquals(id.endsWith("-toll") ? "Pay-toll(10gp)" : "Open", gate.getMenuOption(), id);
        }
    }

    @Test
    void theDatasetStillListsTheGateAsAFreeOpen() {
        List<TransportDefinition> edges = datasetEdgesBetween(Set.of(SOUTH_WEST, SOUTH_EAST, NORTH_WEST, NORTH_EAST));

        assertEquals(4, edges.size(), "expected the four gate edges; delete the gate rows if the dataset dropped them");
        for (TransportDefinition edge : edges) {
            assertEquals("Open", edge.toObjectInfo().getMenuOption(), edge.getObjectInfo());
            assertTrue(edge.getRequirements().isEmpty(),
                    "the dataset now states a requirement for " + edge.getId()
                            + "; delete the gate rows if it is the toll rule");
        }
    }

    @Test
    void theGateRowsHideEveryDatasetEdgeOfTheGate() {
        Set<TransportRegistry.Edge> hidden = new TransportRegistry(gates).snapshot().getDisabledEdges();

        for (TransportDefinition edge : datasetEdgesBetween(Set.of(SOUTH_WEST, SOUTH_EAST, NORTH_WEST, NORTH_EAST))) {
            assertTrue(hidden.contains(new TransportRegistry.Edge(edge.getOrigin(), edge.getDestination())), edge.getId());
        }
    }

    @Test
    void aPlayerWithNoCoinsAndAnUnpaidGateCannotCross() {
        assertTrue(usableGates(state().build()).isEmpty());
        assertTrue(usableGates(state().item(ItemID.COINS, 9).build()).isEmpty());
    }

    @Test
    void tenCoinsPayTheToll() {
        List<TransportDefinition> usable = usableGates(state().item(ItemID.COINS, 10).build());

        assertEquals(2, usable.size(), "one row per gate");
        assertTrue(usable.stream().allMatch(row -> row.getId().endsWith("-toll")));
    }

    @Test
    void aGateThatNoLongerChargesIsOpenedAndNeverPaid() {
        List<TransportDefinition> byVarp = usableGates(
                state().item(ItemID.COINS, 1_000).varPlayer(VarPlayerID.PRINCEQUEST, 100).build());
        List<TransportDefinition> byQuest = usableGates(state().completedQuest(Quest.PRINCE_ALI_RESCUE).build());

        assertEquals(2, byVarp.size());
        assertTrue(byVarp.stream().allMatch(row -> "Open".equals(row.getMenuOption())), byVarp.toString());
        assertEquals(2, byQuest.size());
        assertTrue(byQuest.stream().allMatch(row -> "Open".equals(row.getMenuOption())), byQuest.toString());
    }

    @Test
    void theFreeGateRowsFollowTheTollRowsSoTheyAreTheOnesOperatedWhenBothAreUsable() {
        // The varp is the quest's own progress, so a finished quest with a low varp should not
        // occur. If it did, the toll row and the quest row would both be usable, and the row
        // registered later is the one operated.
        List<TransportDefinition> usable = usableGates(
                state().item(ItemID.COINS, 1_000).completedQuest(Quest.PRINCE_ALI_RESCUE).build());

        assertEquals(4, usable.size());
        for (String gate : new String[]{"south", "north"}) {
            List<TransportDefinition> rows = usable.stream()
                    .filter(row -> row.getId().contains("-" + gate + "-"))
                    .collect(Collectors.toList());
            assertEquals("Open", rows.get(rows.size() - 1).getMenuOption(), rows.toString());
        }
    }

    @Test
    void everyWebRowIsSlashedThenWalkedThroughInBothDirections() {
        assertEquals(7, webs.size());
        for (TransportDefinition web : webs) {
            String id = web.getId();
            assertEquals(TransportShape.CLEAR_THEN_WALK, web.getShape(), id);
            assertEquals("Slash Web 733", web.getObjectInfo(), id);
            assertTrue(web.isBidirectional(), id);
            assertTrue(web.isReplaces(), id + " has to hide the dataset's web, which would be clicked once and left");
        }
    }

    @Test
    void theWebRowsAndTheDatasetsWebsAreTheSameEdgesWithTheSameRequirements() {
        Set<TransportRegistry.Edge> registered = new HashSet<>();
        for (TransportDefinition web : webs) {
            registered.add(new TransportRegistry.Edge(web.getOrigin(), web.getDestination()));
            registered.add(new TransportRegistry.Edge(web.getDestination(), web.getOrigin()));
        }

        Set<TransportRegistry.Edge> slashed = new HashSet<>();
        for (TransportDefinition edge : dataset) {
            if (edge.isUsableAnywhere() || edge.toObjectInfo() == null
                    || !"Slash".equalsIgnoreCase(edge.toObjectInfo().getMenuOption())) {
                continue;
            }
            assertEquals(4, edge.getDuration(), edge.getId());
            slashed.add(new TransportRegistry.Edge(edge.getOrigin(), edge.getDestination()));

            TransportDefinition row = webs.stream()
                    .filter(web -> Set.of(web.getOrigin(), web.getDestination())
                            .equals(Set.of(edge.getOrigin(), edge.getDestination())))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError(edge.getId() + " has no hand written row"));
            assertEquals(edge.getRequirements().getItems(), row.getRequirements().getItems(),
                    row.getId() + " replaces the dataset's web, so it has to ask for the same weapon");
        }

        assertEquals(14, slashed.size());
        assertEquals(slashed, registered,
                "a web the dataset gained needs a row, or it is clicked once and never walked through");
        assertTrue(webs.stream().allMatch(web -> web.getDuration() == 4));
    }

    @Test
    void aWebNeedsSomethingToSlashItWith() {
        TransportDefinition web = webs.get(0);

        assertFalse(TransportRequirements.unmetReasons(web.getRequirements(), state().build()).isEmpty());
        assertTrue(TransportRequirements.unmetReasons(
                web.getRequirements(), state().item(ItemID.BRONZE_AXE, 1).build()).isEmpty());
    }

    @Test
    void aSlashedThenWalkedRowSurvivesTheFileFormat() {
        TransportDefinition web = webs.get(0);

        TransportDefinition readBack = BundledTransports.parse("[" + BundledTransports.toJson(web) + "]").get(0);

        assertEquals(TransportShape.CLEAR_THEN_WALK, readBack.getShape());
        assertEquals(web.getOrigin(), readBack.getOrigin());
        assertEquals(web.getDestination(), readBack.getDestination());
        assertEquals("Slash Web 733", readBack.getObjectInfo());
        assertEquals(web.getRequirements().getItems(), readBack.getRequirements().getItems());
        assertTrue(readBack.isBidirectional());
        assertTrue(readBack.isReplaces());
        assertFalse(readBack.isUsableAnywhere());
    }

    private static List<TransportDefinition> usableGates(TransportRequirements.PlayerState state) {
        return new TransportRegistry(gates).snapshot().usable(DEFAULTS, state);
    }

    private static TransportRequirements.PlayerState.PlayerStateBuilder state() {
        return TransportRequirements.PlayerState.builder();
    }

    private static List<TransportDefinition> datasetEdgesBetween(Set<WorldPoint> tiles) {
        return dataset.stream()
                .filter(edge -> !edge.isUsableAnywhere())
                .filter(edge -> tiles.contains(edge.getOrigin()) && tiles.contains(edge.getDestination()))
                .collect(Collectors.toList());
    }
}
