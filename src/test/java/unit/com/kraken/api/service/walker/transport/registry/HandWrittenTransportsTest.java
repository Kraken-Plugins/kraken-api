package com.kraken.api.service.walker.transport.registry;

import com.kraken.api.service.pathfinding.GlobalPathfinder;
import com.kraken.api.service.pathfinding.GlobalPathfinderConfig;
import com.kraken.api.service.walker.transport.ObjectInfo;
import com.kraken.api.service.walker.transport.TransportRequirements;
import com.kraken.api.service.walker.transport.TransportShape;
import net.runelite.api.Quest;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarPlayerID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import shortestpath.PrimitiveIntHashMap;
import shortestpath.WorldPointUtil;
import shortestpath.transport.Transport;
import shortestpath.transport.TransportLoader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the bundled rows that are written by hand to correct single transports of the dataset: the
 * Al Kharid toll gate, which the dataset lists as a free Open on object ids that are no longer in
 * the scene, and the webs, which are slashed and then walked through.
 *
 * <p>Each of these rows takes a dataset edge over with {@code replaces}. The tests read the pinned
 * dataset, so a dependency bump that moves, removes or corrects one of those edges fails here, where
 * the row can be updated or deleted.</p>
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
    private static List<Transport> dataset;

    @BeforeAll
    static void load() {
        List<TransportDefinition> bundled = BundledTransports.load();
        gates = bundled.stream().filter(row -> row.getId().startsWith(GATE_PREFIX)).collect(Collectors.toList());
        webs = bundled.stream().filter(row -> row.getId().startsWith(WEB_PREFIX)).collect(Collectors.toList());

        dataset = new ArrayList<>();
        for (Set<Transport> transports : TransportLoader.loadAllFromResources().values()) {
            dataset.addAll(transports);
        }
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
    void theDatasetStillListsTheGateAsAFreeOpenOnStaleIds() {
        List<Transport> edges = datasetEdgesBetween(Set.of(SOUTH_WEST, SOUTH_EAST, NORTH_WEST, NORTH_EAST));

        assertEquals(4, edges.size(), "expected the four gate edges; delete the gate rows if the dataset dropped them");
        for (Transport edge : edges) {
            ObjectInfo info = ObjectInfo.parse(edge.getObjectInfo());
            assertEquals("Open", info.getMenuOption(), edge.getObjectInfo());
            assertTrue(info.getId() == 44050 || info.getId() == 44051,
                    "the dataset now names " + info.getId() + "; delete the gate rows if it lists the live gate and its toll");
            assertTrue(edge.getItemRequirements() == null
                            || edge.getItemRequirements().getRequirements() == null
                            || edge.getItemRequirements().getRequirements().isEmpty(),
                    "the dataset now lists a fare for " + edge.getObjectInfo());
            assertTrue(edge.getQuests() == null || edge.getQuests().isEmpty(),
                    "the dataset now lists a quest for " + edge.getObjectInfo());
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
    void whenTheQuestIsDoneAndCoinsAreCarriedTheFreeGateIsTheOnePlanned() {
        // The varp is the quest's own progress, so a finished quest with a low varp should not
        // occur. If it did, the toll row and the quest row would both be usable, and the row
        // registered later is the one operated: the quest rows follow the toll rows in the file.
        TransportRequirements.PlayerState state =
                state().item(ItemID.COINS, 1_000).completedQuest(Quest.PRINCE_ALI_RESCUE).build();
        PrimitiveIntHashMap<Transport[]> packed = new PrimitiveIntHashMap<>(8);

        ShortestPathBridge.Injection injection = ShortestPathBridge.inject(usableGates(state), Set.of(), packed, packed);

        for (WorldPoint[] crossing : new WorldPoint[][]{
                {SOUTH_WEST, SOUTH_EAST}, {SOUTH_EAST, SOUTH_WEST}, {NORTH_WEST, NORTH_EAST}, {NORTH_EAST, NORTH_WEST}}) {
            TransportDefinition planned = injection.definitionOf(hop(packed, crossing[0], crossing[1]));
            assertNotNull(planned);
            assertEquals("Open", planned.getMenuOption(), planned.getId());
        }
    }

    @Test
    void theDatasetsGateIsHiddenEvenWhenThePlayerCannotCross() {
        TransportRegistry registry = new TransportRegistry(gates);
        PrimitiveIntHashMap<Transport[]> packed = packedDataset();
        List<TransportDefinition> usable = registry.snapshot().usable(DEFAULTS, state().build());

        ShortestPathBridge.inject(usable, registry.snapshot().getDisabledEdges(), packed, packed);

        assertTrue(usable.isEmpty());
        assertNull(hop(packed, SOUTH_WEST, SOUTH_EAST), "a broke player must not be routed through the gate");
        assertNull(hop(packed, SOUTH_EAST, SOUTH_WEST));
        assertNull(hop(packed, NORTH_WEST, NORTH_EAST));
        assertNull(hop(packed, NORTH_EAST, NORTH_WEST));
    }

    @Test
    void aPlayerWhoCanPayIsRoutedThroughTheRegisteredGateAndNotTheDatasets() {
        TransportRegistry registry = new TransportRegistry(gates);
        PrimitiveIntHashMap<Transport[]> packed = packedDataset();
        List<TransportDefinition> usable =
                registry.snapshot().usable(DEFAULTS, state().item(ItemID.COINS, 10).build());

        ShortestPathBridge.Injection injection =
                ShortestPathBridge.inject(usable, registry.snapshot().getDisabledEdges(), packed, packed);

        TransportDefinition planned = injection.definitionOf(hop(packed, SOUTH_EAST, SOUTH_WEST));
        assertNotNull(planned, "the return direction is planned through the same bidirectional row");
        assertEquals("kraken:al-kharid-gate-south-toll", planned.getId());
        assertEquals("Pay-toll(10gp) Gate 44598", planned.getObjectInfo());
        long gateEdges = Arrays.stream(packed.get(WorldPointUtil.packWorldPoint(SOUTH_EAST)))
                .filter(edge -> edge.getDestination() == WorldPointUtil.packWorldPoint(SOUTH_WEST))
                .count();
        assertEquals(1, gateEdges, "the dataset's own edge between the same tiles is gone");
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
            assertTrue(web.getRequirements().isEmpty(), id);
        }
    }

    @Test
    void theWebRowsAndTheDatasetsWebsAreTheSameEdges() {
        Set<TransportRegistry.Edge> registered = new HashSet<>();
        for (TransportDefinition web : webs) {
            registered.add(new TransportRegistry.Edge(web.getOrigin(), web.getDestination()));
            registered.add(new TransportRegistry.Edge(web.getDestination(), web.getOrigin()));
        }

        Set<TransportRegistry.Edge> slashed = new HashSet<>();
        for (Transport edge : dataset) {
            ObjectInfo info = ObjectInfo.parse(edge.getObjectInfo());
            if (info != null && "Slash".equalsIgnoreCase(info.getMenuOption())) {
                assertEquals(4, edge.getDuration(), edge.getObjectInfo());
                slashed.add(new TransportRegistry.Edge(
                        WorldPointUtil.unpackWorldPoint(edge.getOrigin()),
                        WorldPointUtil.unpackWorldPoint(edge.getDestination())));
            }
        }

        assertEquals(14, slashed.size());
        assertEquals(slashed, registered,
                "a web the dataset gained needs a row, or it is clicked once and never walked through");
        assertTrue(webs.stream().allMatch(web -> web.getDuration() == 4));
    }

    @Test
    void theDatasetsWebsAreHiddenAndTheRegisteredOnesPlanned() {
        TransportRegistry registry = new TransportRegistry(webs);
        PrimitiveIntHashMap<Transport[]> packed = packedDataset();

        ShortestPathBridge.Injection injection = ShortestPathBridge.inject(
                registry.snapshot().usable(DEFAULTS, state().build()),
                registry.snapshot().getDisabledEdges(), packed, packed);

        for (TransportDefinition web : webs) {
            for (WorldPoint[] crossing : new WorldPoint[][]{
                    {web.getOrigin(), web.getDestination()}, {web.getDestination(), web.getOrigin()}}) {
                int origin = WorldPointUtil.packWorldPoint(crossing[0]);
                int destination = WorldPointUtil.packWorldPoint(crossing[1]);
                long edges = Arrays.stream(packed.get(origin))
                        .filter(edge -> edge.getDestination() == destination)
                        .count();

                assertEquals(1, edges, web.getId() + " shares its tiles with a dataset edge");
                assertEquals(web, injection.definitionOf(hop(packed, crossing[0], crossing[1])));
            }
        }
    }

    @Test
    void aSlashedThenWalkedRowSurvivesTheFileFormat() {
        TransportDefinition web = webs.get(0);

        TransportDefinition readBack = BundledTransports.parse("[" + BundledTransports.toJson(web) + "]").get(0);

        assertEquals(TransportShape.CLEAR_THEN_WALK, readBack.getShape());
        assertEquals(web.getOrigin(), readBack.getOrigin());
        assertEquals(web.getDestination(), readBack.getDestination());
        assertEquals("Slash Web 733", readBack.getObjectInfo());
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

    private static List<Transport> datasetEdgesBetween(Set<WorldPoint> tiles) {
        Set<Integer> packed = tiles.stream().map(WorldPointUtil::packWorldPoint).collect(Collectors.toSet());
        return dataset.stream()
                .filter(edge -> packed.contains(edge.getOrigin()) && packed.contains(edge.getDestination()))
                .collect(Collectors.toList());
    }

    /**
     * Builds the planner's origin to edges map from every dataset edge that has an origin tile.
     */
    private static PrimitiveIntHashMap<Transport[]> packedDataset() {
        PrimitiveIntHashMap<Transport[]> packed = new PrimitiveIntHashMap<>(dataset.size());
        for (Transport edge : dataset) {
            if (edge.getOrigin() == WorldPointUtil.UNDEFINED) {
                continue;
            }
            Transport[] current = packed.get(edge.getOrigin());
            Transport[] combined = new Transport[current == null ? 1 : current.length + 1];
            if (current != null) {
                System.arraycopy(current, 0, combined, 0, current.length);
            }
            combined[combined.length - 1] = edge;
            packed.put(edge.getOrigin(), combined);
        }
        return packed;
    }

    private static Transport hop(PrimitiveIntHashMap<Transport[]> packed, WorldPoint from, WorldPoint to) {
        int origin = WorldPointUtil.packWorldPoint(from);
        return GlobalPathfinder.hopBetween(packed.get(origin), new Transport[0], origin, WorldPointUtil.packWorldPoint(to));
    }
}
