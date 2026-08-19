package unit.com.kraken.api.service.walker.transport;

import com.kraken.api.service.walker.transport.AlKharidGate;
import com.kraken.api.service.walker.transport.TransportRequirements;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import shortestpath.PrimitiveIntHashMap;
import shortestpath.transport.Transport;
import shortestpath.transport.TransportLoader;
import shortestpath.transport.requirement.TransportItems;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the Al Kharid toll overlay. The shortest-path dataset lists those four tiles as a free
 * Open; without the overlay a broke F2P account is routed through a gate that will not open.
 */
class AlKharidGateTest {

    private static List<Transport> gates;

    @BeforeAll
    static void loadGates() {
        gates = new ArrayList<>();
        for (Set<Transport> set : TransportLoader.loadAllFromResources().values()) {
            for (Transport transport : set) {
                if (AlKharidGate.matches(transport)) {
                    gates.add(transport);
                }
            }
        }
    }

    @Test
    void theDatasetStillTreatsTheGateAsFree() {
        assertEquals(4, gates.size(), "expected the four Open Gate edges; delete the overlay if the TSV gained fares");

        for (Transport gate : gates) {
            TransportItems items = gate.getItemRequirements();
            assertTrue(items == null || items.getRequirements() == null || items.getRequirements().isEmpty(),
                    "gate " + gate + " unexpectedly lists items");
            assertTrue(gate.getQuests() == null || gate.getQuests().isEmpty(),
                    "gate " + gate + " unexpectedly lists quests");
        }
    }

    @Test
    void zeroCoinsAndAnUnpaidQuestCannotCross() {
        assertFalse(AlKharidGate.usable(0, 0, false));
        assertFalse(AlKharidGate.usable(9, 0, false));
    }

    @Test
    void tenCoinsPaysTheToll() {
        assertTrue(AlKharidGate.usable(10, 0, false));
    }

    @Test
    void aFinishedQuestOpensTheGateForFree() {
        assertTrue(AlKharidGate.usable(0, 0, true));
        assertTrue(AlKharidGate.usable(0, 100, false));
    }

    @Test
    void requirementsReportTheFareOnAMatchingEdge() {
        Transport gate = gates.get(0);
        TransportRequirements.PlayerState broke = maxedPlayer().build();

        List<String> reasons = TransportRequirements.unmetReasons(gate, broke);

        assertTrue(reasons.stream().anyMatch(r -> r.contains("10 coins") && r.contains("Al Kharid")),
                reasons.toString());
        assertFalse(TransportRequirements.met(gate, broke));
    }

    @Test
    void carryingTheTollSatisfiesTheOverlay() {
        Transport gate = gates.get(0);
        TransportRequirements.PlayerState paid = maxedPlayer().item(ItemID.COINS, 10).build();

        assertTrue(TransportRequirements.met(gate, paid),
                TransportRequirements.unmetReasons(gate, paid).toString());
    }

    @Test
    void completingPrinceAliRescueSatisfiesTheOverlay() {
        Transport gate = gates.get(0);
        TransportRequirements.PlayerState rescued = maxedPlayer()
                .completedQuest(Quest.PRINCE_ALI_RESCUE)
                .build();

        assertTrue(TransportRequirements.met(gate, rescued),
                TransportRequirements.unmetReasons(gate, rescued).toString());
    }

    @Test
    void aHighGateVarpSatisfiesTheOverlay() {
        Transport gate = gates.get(0);
        TransportRequirements.PlayerState free = maxedPlayer()
                .varPlayer(AlKharidGate.GATE_VARP, 100)
                .build();

        assertTrue(TransportRequirements.met(gate, free),
                TransportRequirements.unmetReasons(gate, free).toString());
    }

    @Test
    void aDifferentTransportIsUnchanged() {
        Transport other = null;
        for (Set<Transport> set : TransportLoader.loadAllFromResources().values()) {
            for (Transport transport : set) {
                if (!AlKharidGate.matches(transport)
                        && (transport.getQuests() == null || transport.getQuests().isEmpty())
                        && transport.getItemRequirements() == null
                        && (transport.getVarRequirements() == null || transport.getVarRequirements().isEmpty())
                        && transport.getType() == shortestpath.transport.TransportType.TRANSPORT) {
                    other = transport;
                    break;
                }
            }
            if (other != null) {
                break;
            }
        }
        assertNotNull(other);

        assertTrue(TransportRequirements.met(other, maxedPlayer().build()),
                TransportRequirements.unmetReasons(other, maxedPlayer().build()).toString());
        assertTrue(AlKharidGate.unmetReasons(other, maxedPlayer().build()).isEmpty());
    }

    @Test
    void stripRemovesTheGateAndLeavesOtherOriginsAlone() {
        Transport gate = gates.get(0);
        PrimitiveIntHashMap<Transport[]> packed = new PrimitiveIntHashMap<>(8);
        packed.put(gate.getOrigin(), new Transport[]{gate});

        AlKharidGate.stripIfUnusable(packed, packed, 0, 0, false);

        Transport[] remaining = packed.get(gate.getOrigin());
        assertNotNull(remaining);
        assertEquals(0, remaining.length);
    }

    @Test
    void stripLeavesTheGateWhenItIsPayable() {
        Transport gate = gates.get(0);
        PrimitiveIntHashMap<Transport[]> packed = new PrimitiveIntHashMap<>(8);
        packed.put(gate.getOrigin(), new Transport[]{gate});

        AlKharidGate.stripIfUnusable(packed, packed, 10, 0, false);

        Transport[] remaining = packed.get(gate.getOrigin());
        assertNotNull(remaining);
        assertEquals(1, remaining.length);
    }

    @Test
    void unpaidSouthGateClicksPayTollOnTheLiveId() {
        int south = shortestpath.WorldPointUtil.packWorldPoint(3267, 3227, 0);

        assertEquals("Pay-toll(10gp) Gate 44598", AlKharidGate.liveObjectInfo(south, false));
        assertEquals("Open Gate 44598", AlKharidGate.liveObjectInfo(south, true));
    }

    @Test
    void unpaidNorthGateClicksPayTollOnTheLiveId() {
        int north = shortestpath.WorldPointUtil.packWorldPoint(3267, 3228, 0);

        assertEquals("Pay-toll(10gp) Gate 44599", AlKharidGate.liveObjectInfo(north, false));
        assertEquals("Open Gate 44599", AlKharidGate.liveObjectInfo(north, true));
    }

    @Test
    void aMatchingEdgeIsRewrittenAndADifferentTransportIsLeftAlone() {
        Transport gate = gates.get(0);
        assertEquals("Pay-toll(10gp) Gate " + (shortestpath.WorldPointUtil.unpackWorldY(gate.getOrigin()) == 3228
                        ? AlKharidGate.NORTH_OBJECT_ID : AlKharidGate.SOUTH_OBJECT_ID),
                AlKharidGate.liveObjectInfo(gate, false));

        TransportRequirements.PlayerState unpaid = maxedPlayer().item(ItemID.COINS, 10).build();
        assertTrue(AlKharidGate.liveObjectInfo(gate, unpaid).startsWith("Pay-toll(10gp) Gate "));

        TransportRequirements.PlayerState free = maxedPlayer()
                .completedQuest(Quest.PRINCE_ALI_RESCUE)
                .build();
        assertTrue(AlKharidGate.liveObjectInfo(gate, free).startsWith("Open Gate "));
    }

    private static TransportRequirements.PlayerState.PlayerStateBuilder maxedPlayer() {
        int[] levels = new int[Skill.values().length];
        for (int i = 0; i < levels.length; i++) {
            levels[i] = 99;
        }
        return TransportRequirements.PlayerState.builder().skillLevels(levels);
    }
}
