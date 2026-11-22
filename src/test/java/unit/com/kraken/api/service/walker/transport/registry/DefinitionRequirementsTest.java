package unit.com.kraken.api.service.walker.transport.registry;

import com.kraken.api.service.walker.transport.TransportRequirements;
import com.kraken.api.service.walker.transport.registry.DefinitionRequirements;
import com.kraken.api.service.walker.transport.registry.DefinitionRequirements.Comparison;
import com.kraken.api.service.walker.transport.registry.DefinitionRequirements.RequiredItem;
import com.kraken.api.service.walker.transport.registry.DefinitionRequirements.RequiredVar;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the requirement check that keeps a registered transport out of a route, and out of the
 * walker's hands, when the player cannot use it.
 */
class DefinitionRequirementsTest {

    @Test
    void noRequirementsAreAlwaysMet() {
        assertTrue(DefinitionRequirements.NONE.isEmpty());
        assertTrue(TransportRequirements.unmetReasons(DefinitionRequirements.NONE, state().build()).isEmpty());
    }

    @Test
    void aSkillLevelBelowTheRequirementIsNamed() {
        DefinitionRequirements requirements = DefinitionRequirements.builder().skill(Skill.AGILITY, 50).build();

        List<String> reasons = TransportRequirements.unmetReasons(requirements, state().skillLevels(levels(49)).build());

        assertEquals(1, reasons.size());
        assertTrue(reasons.get(0).contains("50") && reasons.get(0).contains("Agility"), reasons.get(0));
        assertTrue(TransportRequirements.unmetReasons(requirements, state().skillLevels(levels(50)).build()).isEmpty());
    }

    @Test
    void anUnreadSkillCountsAsLevelZero() {
        DefinitionRequirements requirements = DefinitionRequirements.builder().skill(Skill.AGILITY, 1).build();

        assertFalse(TransportRequirements.unmetReasons(requirements, state().build()).isEmpty());
    }

    @Test
    void anUnfinishedQuestIsNamed() {
        DefinitionRequirements requirements =
                DefinitionRequirements.builder().quest(Quest.CHILDREN_OF_THE_SUN).build();

        List<String> reasons = TransportRequirements.unmetReasons(requirements, state().build());

        assertEquals(1, reasons.size());
        assertTrue(reasons.get(0).contains(Quest.CHILDREN_OF_THE_SUN.getName()), reasons.get(0));
        assertTrue(TransportRequirements.unmetReasons(requirements,
                state().completedQuest(Quest.CHILDREN_OF_THE_SUN).build()).isEmpty());
    }

    @Test
    void aFareNeedsTheFullAmount() {
        DefinitionRequirements requirements =
                DefinitionRequirements.builder().item(RequiredItem.of(ItemID.COINS, 10_000)).build();

        assertFalse(TransportRequirements.unmetReasons(requirements,
                state().item(ItemID.COINS, 9_999).build()).isEmpty());
        assertTrue(TransportRequirements.unmetReasons(requirements,
                state().item(ItemID.COINS, 10_000).build()).isEmpty());
    }

    @Test
    void anyOneOfSeveralInterchangeableItemsIsEnough() {
        DefinitionRequirements requirements = DefinitionRequirements.builder()
                .item(RequiredItem.anyOf(ItemID.DRAMEN_STAFF, ItemID.LUNAR_MOONCLAN_LIMINAL_STAFF))
                .build();

        assertFalse(TransportRequirements.unmetReasons(requirements, state().build()).isEmpty());
        assertTrue(TransportRequirements.unmetReasons(requirements,
                state().item(ItemID.LUNAR_MOONCLAN_LIMINAL_STAFF, 1).build()).isEmpty());
    }

    @Test
    void everyItemEntryMustBeHeld() {
        DefinitionRequirements requirements = DefinitionRequirements.builder()
                .item(RequiredItem.of(ItemID.COINS, 10))
                .item(RequiredItem.of(ItemID.ROPE))
                .build();

        assertEquals(1, TransportRequirements.unmetReasons(requirements,
                state().item(ItemID.COINS, 10).build()).size());
    }

    @Test
    void varbitsAndVarPlayersAreReadFromTheirOwnMaps() {
        DefinitionRequirements requirements = DefinitionRequirements.builder()
                .varbit(new RequiredVar(100, Comparison.AT_LEAST, 7))
                .varPlayer(new RequiredVar(100, Comparison.EQUAL, 1))
                .build();

        assertEquals(2, TransportRequirements.unmetReasons(requirements, state().build()).size());
        assertEquals(1, TransportRequirements.unmetReasons(requirements, state().varbit(100, 7).build()).size());
        assertTrue(TransportRequirements.unmetReasons(requirements,
                state().varbit(100, 9).varPlayer(100, 1).build()).isEmpty());
    }

    @Test
    void aMembersOnlyTransportNeedsAMembersWorld() {
        DefinitionRequirements requirements = DefinitionRequirements.builder().membersOnly(true).build();

        assertFalse(requirements.isEmpty());
        assertFalse(TransportRequirements.unmetReasons(requirements, state().build()).isEmpty());
        assertTrue(TransportRequirements.unmetReasons(requirements, state().membersWorld(true).build()).isEmpty());
    }

    @Test
    void requirementsAreNotMetWhenThePlayerStateCouldNotBeRead() {
        DefinitionRequirements requirements = DefinitionRequirements.builder().skill(Skill.AGILITY, 1).build();

        assertFalse(TransportRequirements.unmetReasons(requirements, null).isEmpty());
        assertTrue(TransportRequirements.unmetReasons(DefinitionRequirements.NONE, null).isEmpty());
        assertTrue(TransportRequirements.unmetReasons((DefinitionRequirements) null, null).isEmpty());
    }

    @Test
    void comparisonsCoverEveryDirection() {
        assertTrue(Comparison.EQUAL.test(3, 3));
        assertFalse(Comparison.EQUAL.test(3, 4));
        assertTrue(Comparison.NOT_EQUAL.test(3, 4));
        assertFalse(Comparison.NOT_EQUAL.test(3, 3));
        assertTrue(Comparison.AT_LEAST.test(4, 3));
        assertFalse(Comparison.AT_LEAST.test(2, 3));
        assertTrue(Comparison.AT_MOST.test(3, 3));
        assertFalse(Comparison.AT_MOST.test(4, 3));
        assertTrue(Comparison.BIT_SET.test(0b100, 2));
        assertFalse(Comparison.BIT_SET.test(0b100, 1));
        assertFalse(Comparison.BIT_SET.test(-1, 32));
    }

    @Test
    void joiningTwoSetsOfRequirementsAsksForBoth() {
        RequiredItem coins = RequiredItem.of(ItemID.COINS, 30);
        RequiredVar unlocked = new RequiredVar(100, Comparison.EQUAL, 1);
        DefinitionRequirements stop = DefinitionRequirements.builder()
                .skill(Skill.AGILITY, 40).skill(Skill.MAGIC, 10)
                .combatLevel(50)
                .quest(Quest.DRAGON_SLAYER_I)
                .item(coins)
                .varbit(unlocked)
                .build();
        DefinitionRequirements other = DefinitionRequirements.builder()
                .skill(Skill.AGILITY, 60).skill(Skill.STRENGTH, 20)
                .combatLevel(30)
                .totalLevel(DefinitionRequirements.MAX_TOTAL_LEVEL)
                .quest(Quest.DRAGON_SLAYER_I).quest(Quest.PRINCE_ALI_RESCUE)
                .item(RequiredItem.of(ItemID.COINS, 30)).item(RequiredItem.of(ItemID.DRAMEN_STAFF))
                .varbit(new RequiredVar(100, Comparison.EQUAL, 1))
                .varPlayer(new RequiredVar(273, Comparison.AT_LEAST, 100))
                .membersOnly(true)
                .build();

        DefinitionRequirements both = stop.and(other);

        assertEquals(60, both.getSkillLevels().get(Skill.AGILITY), "the higher of two levels is kept");
        assertEquals(10, both.getSkillLevels().get(Skill.MAGIC));
        assertEquals(20, both.getSkillLevels().get(Skill.STRENGTH));
        assertEquals(50, both.getCombatLevel());
        assertEquals(DefinitionRequirements.MAX_TOTAL_LEVEL, both.getTotalLevel(), "the highest total level wins over any number");
        assertEquals(2, both.getQuests().size());
        assertEquals(List.of(coins, RequiredItem.of(ItemID.DRAMEN_STAFF)), both.getItems(), "a requirement both state is asked once");
        assertEquals(List.of(unlocked), both.getVarbits());
        assertEquals(1, both.getVarPlayers().size());
        assertTrue(both.isMembersOnly());
        assertEquals(40, stop.getSkillLevels().get(Skill.AGILITY), "the originals are left as they were");
    }

    @Test
    void joiningWithNothingGivesBackTheSameRequirements() {
        DefinitionRequirements stop = DefinitionRequirements.builder().skill(Skill.AGILITY, 40).build();

        assertTrue(stop.and(DefinitionRequirements.NONE) == stop);
        assertTrue(stop.and(null) == stop);
        assertTrue(DefinitionRequirements.NONE.and(stop) == stop);
    }

    @Test
    void theNewComparisonsReadAMaskAndACooldown() {
        assertTrue(Comparison.ANY_BIT_OF.test(0b0100, 0b0110));
        assertFalse(Comparison.ANY_BIT_OF.test(0b1001, 0b0110));
        assertFalse(Comparison.COOLDOWN_MINUTES.test(1_000, 30, 1_030), "exactly thirty minutes is not yet more than thirty");
        assertTrue(Comparison.COOLDOWN_MINUTES.test(1_000, 30, 1_031));
        assertTrue(Comparison.COOLDOWN_MINUTES.test(0, 30), "something never used has cooled down");
    }

    private static TransportRequirements.PlayerState.PlayerStateBuilder state() {
        return TransportRequirements.PlayerState.builder();
    }

    private static int[] levels(int agility) {
        int[] levels = new int[Skill.values().length];
        levels[Skill.AGILITY.ordinal()] = agility;
        return levels;
    }
}
