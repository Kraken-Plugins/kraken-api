package unit.com.kraken.api.service.walker.transport;

import com.kraken.api.service.walker.transport.TransportRequirements;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import net.runelite.api.gameval.ItemID;
import shortestpath.transport.Transport;
import shortestpath.transport.TransportType;
import shortestpath.transport.TransportLoader;
import shortestpath.transport.requirement.ItemRequirement;
import shortestpath.transport.requirement.TransportItems;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the gate that stops the walker clicking a transport it cannot actually use.
 *
 * <p>Transports are read from the dataset the pathfinder itself loads rather than hand-built, because
 * their constructor is not visible outside the library. Each test picks a transport by the shape of
 * its requirements rather than by identity, so the suite survives dataset updates.</p>
 */
class TransportRequirementsTest {

    private static List<Transport> transports;

    @BeforeAll
    static void loadTransports() {
        transports = new ArrayList<>();
        for (Set<Transport> set : TransportLoader.loadAllFromResources().values()) {
            transports.addAll(set);
        }
    }

    private static Transport find(Predicate<Transport> predicate) {
        return transports.stream().filter(predicate).findFirst().orElse(null);
    }

    private static TransportRequirements.PlayerState.PlayerStateBuilder maxedPlayer() {
        int[] levels = new int[Skill.values().length];
        for (int i = 0; i < levels.length; i++) {
            levels[i] = 99;
        }
        return TransportRequirements.PlayerState.builder().skillLevels(levels);
    }

    @Test
    void theDatasetLoads() {
        assertFalse(transports.isEmpty());
    }

    @Test
    void aTransportWithNoRequirementsIsUsable() {
        Transport open = find(t -> (t.getQuests() == null || t.getQuests().isEmpty())
                && t.getItemRequirements() == null
                && (t.getVarRequirements() == null || t.getVarRequirements().isEmpty())
                && noSkillRequirement(t));
        assertNotNull(open, "expected at least one unrestricted transport");

        assertTrue(TransportRequirements.met(open, maxedPlayer().build()));
    }

    @Test
    void aNullTransportIsTreatedAsUsable() {
        assertTrue(TransportRequirements.met(null, maxedPlayer().build()));
    }

    @Test
    void anUnfinishedQuestIsReported() {
        Transport questLocked = find(t -> t.getQuests() != null && !t.getQuests().isEmpty());
        assertNotNull(questLocked, "expected at least one quest locked transport");

        List<String> reasons = TransportRequirements.unmetReasons(questLocked, maxedPlayer().build());

        assertFalse(reasons.isEmpty());
        assertTrue(reasons.stream().anyMatch(r -> r.startsWith("needs quest ")), reasons.toString());
    }

    @Test
    void aFinishedQuestSatisfiesTheRequirement() {
        Transport questLocked = find(t -> t.getQuests() != null && t.getQuests().size() == 1
                && t.getItemRequirements() == null
                && (t.getVarRequirements() == null || t.getVarRequirements().isEmpty())
                && noSkillRequirement(t));
        assertNotNull(questLocked, "expected a transport gated only by one quest");

        Quest quest = questLocked.getQuests().iterator().next();
        TransportRequirements.PlayerState state = maxedPlayer().completedQuest(quest).build();

        assertTrue(TransportRequirements.met(questLocked, state),
                TransportRequirements.unmetReasons(questLocked, state).toString());
    }

    @Test
    void aMissingItemIsReported() {
        Transport needsItem = find(t -> hasItemRequirement(t));
        assertNotNull(needsItem, "expected at least one transport requiring an item");

        List<String> reasons = TransportRequirements.unmetReasons(needsItem, maxedPlayer().build());

        assertTrue(reasons.stream().anyMatch(r -> r.startsWith("missing a required item")), reasons.toString());
    }

    @Test
    void holdingTheItemSatisfiesTheRequirement() {
        Transport needsItem = find(t -> hasItemRequirement(t)
                && (t.getQuests() == null || t.getQuests().isEmpty())
                && (t.getVarRequirements() == null || t.getVarRequirements().isEmpty())
                && noSkillRequirement(t));
        assertNotNull(needsItem, "expected a transport gated only by items");

        TransportRequirements.PlayerState.PlayerStateBuilder builder = maxedPlayer();
        for (ItemRequirement requirement : needsItem.getItemRequirements().getRequirements()) {
            int[] ids = requirement.getItemIds();
            if (ids != null && ids.length > 0) {
                builder.item(ids[0], Math.max(requirement.getQuantity(), 1));
            }
        }

        TransportRequirements.PlayerState state = builder.build();
        assertTrue(TransportRequirements.met(needsItem, state),
                TransportRequirements.unmetReasons(needsItem, state).toString());
    }

    @Test
    void anInsufficientQuantityIsReported() {
        Transport needsMany = find(t -> {
            if (!hasItemRequirement(t)) {
                return false;
            }
            return t.getItemRequirements().getRequirements().stream().anyMatch(r -> r.getQuantity() > 1);
        });
        assertNotNull(needsMany, "expected a transport requiring more than one of something");

        TransportRequirements.PlayerState.PlayerStateBuilder builder = maxedPlayer();
        for (ItemRequirement requirement : needsMany.getItemRequirements().getRequirements()) {
            int[] ids = requirement.getItemIds();
            if (ids != null && ids.length > 0) {
                builder.item(ids[0], 1);
            }
        }

        List<String> reasons = TransportRequirements.unmetReasons(needsMany, builder.build());

        assertTrue(reasons.stream().anyMatch(r -> r.startsWith("missing a required item")), reasons.toString());
    }

    @Test
    void aSkillLevelShortfallIsReported() {
        Transport needsSkill = find(TransportRequirementsTest::hasCheckableSkillRequirement);
        assertNotNull(needsSkill, "expected at least one transport with a skill requirement");

        TransportRequirements.PlayerState noSkills = TransportRequirements.PlayerState.builder()
                .skillLevels(new int[Skill.values().length])
                .build();

        List<String> reasons = TransportRequirements.unmetReasons(needsSkill, noSkills);

        assertTrue(reasons.stream().anyMatch(r -> r.startsWith("needs ")), reasons.toString());
    }

    @Test
    void aMaxedPlayerMeetsEverySkillRequirement() {
        Transport needsSkill = find(TransportRequirementsTest::hasCheckableSkillRequirement);
        assertNotNull(needsSkill);

        List<String> reasons = TransportRequirements.unmetReasons(needsSkill, maxedPlayer().build());

        assertTrue(reasons.stream().noneMatch(r -> r.startsWith("needs ") && r.contains(", have ")), reasons.toString());
    }

    @Test
    void aFairyRingNeedsAStaff() {
        Transport ring = find(t -> t.getType() == TransportType.FAIRY_RING);
        assertNotNull(ring, "expected fairy ring transports in the dataset");

        List<String> reasons = TransportRequirements.unmetReasons(ring, maxedPlayer().build());

        assertTrue(reasons.stream().anyMatch(r -> r.contains("dramen")), reasons.toString());
    }

    @Test
    void aDramenStaffSatisfiesAFairyRing() {
        Transport ring = find(TransportRequirementsTest::isUnencumberedFairyRing);
        assertNotNull(ring, "expected a fairy ring gated only by the staff rule");

        TransportRequirements.PlayerState state = maxedPlayer().item(ItemID.DRAMEN_STAFF, 1).build();

        assertTrue(TransportRequirements.met(ring, state),
                TransportRequirements.unmetReasons(ring, state).toString());
    }

    @Test
    void theLumbridgeEliteDiaryReplacesTheStaff() {
        Transport ring = find(TransportRequirementsTest::isUnencumberedFairyRing);
        assertNotNull(ring, "expected a fairy ring gated only by the staff rule");

        TransportRequirements.PlayerState state = maxedPlayer()
                .varbit(TransportRequirements.FAIRY_RING_DIARY_VARBIT, 1)
                .build();

        assertTrue(TransportRequirements.met(ring, state),
                TransportRequirements.unmetReasons(ring, state).toString());
    }

    @Test
    void theStaffRuleOnlyAppliesToFairyRings() {
        Transport door = find(t -> t.getType() == TransportType.TRANSPORT);
        assertNotNull(door);

        List<String> reasons = TransportRequirements.unmetReasons(door, maxedPlayer().build());

        assertTrue(reasons.stream().noneMatch(r -> r.contains("dramen")), reasons.toString());
    }

    /** A fairy ring carrying no requirement of its own, so only the staff rule can block it. */
    private static boolean isUnencumberedFairyRing(Transport transport) {
        return transport.getType() == TransportType.FAIRY_RING
                && (transport.getQuests() == null || transport.getQuests().isEmpty())
                && transport.getItemRequirements() == null
                && (transport.getVarRequirements() == null || transport.getVarRequirements().isEmpty())
                && noSkillRequirement(transport);
    }

    private static boolean noSkillRequirement(Transport transport) {
        return !hasCheckableSkillRequirement(transport);
    }

    private static boolean hasCheckableSkillRequirement(Transport transport) {
        int[] levels = transport.getSkillLevels();
        if (levels == null) {
            return false;
        }

        int checkable = Math.min(levels.length, Skill.values().length);
        for (int i = 0; i < checkable; i++) {
            if (levels[i] > 0) {
                return true;
            }
        }

        return false;
    }

    private static boolean hasItemRequirement(Transport transport) {
        TransportItems items = transport.getItemRequirements();
        return items != null && items.getRequirements() != null && !items.getRequirements().isEmpty();
    }
}
