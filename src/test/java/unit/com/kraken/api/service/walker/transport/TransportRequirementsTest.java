package unit.com.kraken.api.service.walker.transport;

import com.kraken.api.service.walker.transport.TransportRequirements;
import com.kraken.api.service.walker.transport.registry.DefinitionRequirements;
import com.kraken.api.service.walker.transport.registry.DefinitionRequirements.Comparison;
import com.kraken.api.service.walker.transport.registry.DefinitionRequirements.RequiredItem;
import com.kraken.api.service.walker.transport.registry.DefinitionRequirements.RequiredVar;
import com.kraken.api.service.walker.transport.registry.TransportCategory;
import com.kraken.api.service.walker.transport.registry.TransportDataset;
import com.kraken.api.service.walker.transport.registry.TransportDefinition;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the gate that stops the walker planning through or clicking a transport the player cannot
 * actually use.
 *
 * <p>The rules are checked on requirements built here, and then on transports picked from the
 * bundled dataset by the kind of requirement they carry, so the suite survives a regenerated
 * dataset.</p>
 */
class TransportRequirementsTest {

    private static final int AIR_RUNE = 556;
    private static final int FIRE_RUNE = 554;
    private static final int LAW_RUNE = 563;
    private static final int STAFF_OF_AIR = 1381;
    private static final int STAFF_OF_FIRE = 1387;
    private static final int SMOKE_BATTLESTAFF = 11998;
    private static final int TOME_OF_FIRE = 20714;
    private static final int FAIRY_RING_DIARY_VARBIT = 4498;

    private static final RequiredItem AIR_RUNES = RequiredItem.builder()
            .anyOf(new int[]{AIR_RUNE}).quantity(3).staves(new int[]{STAFF_OF_AIR, SMOKE_BATTLESTAFF}).build();
    private static final RequiredItem FIRE_RUNES = RequiredItem.builder()
            .anyOf(new int[]{FIRE_RUNE}).quantity(1).staves(new int[]{STAFF_OF_FIRE, SMOKE_BATTLESTAFF})
            .offhands(new int[]{TOME_OF_FIRE}).build();
    private static final DefinitionRequirements VARROCK_TELEPORT = DefinitionRequirements.builder()
            .item(AIR_RUNES).item(FIRE_RUNES).item(RequiredItem.of(LAW_RUNE)).build();

    private static TransportRequirements.PlayerState.PlayerStateBuilder player() {
        return TransportRequirements.PlayerState.builder().skillLevels(new int[Skill.values().length]);
    }

    private static TransportRequirements.PlayerState.PlayerStateBuilder maxedPlayer() {
        int[] levels = new int[Skill.values().length];
        java.util.Arrays.fill(levels, 99);
        return TransportRequirements.PlayerState.builder()
                .skillLevels(levels)
                .combatLevel(126)
                .totalLevel(TransportRequirements.highestTotalLevel());
    }

    private static boolean met(DefinitionRequirements requirements, TransportRequirements.PlayerState state) {
        return TransportRequirements.unmetReasons(requirements, state).isEmpty();
    }

    private static TransportDefinition datasetRow(Predicate<TransportDefinition> wanted) {
        return TransportDataset.bundled().stream().filter(wanted).findFirst()
                .orElseThrow(() -> new AssertionError("the dataset has no such transport"));
    }

    @Test
    void noRequirementsAreAlwaysMetAndRequirementsWithoutAStateNever() {
        assertTrue(met(DefinitionRequirements.NONE, null));
        assertTrue(met(null, player().build()));
        assertEquals(List.of("player state could not be read"),
                TransportRequirements.unmetReasons(VARROCK_TELEPORT, null));
    }

    @Test
    void aSkillShortfallAnUnfinishedQuestAndAMissingItemAreEachNamed() {
        DefinitionRequirements requirements = DefinitionRequirements.builder()
                .skill(Skill.AGILITY, 50)
                .quest(Quest.DRAGON_SLAYER_I)
                .item(RequiredItem.of(ItemID.COINS, 30))
                .build();

        List<String> reasons = TransportRequirements.unmetReasons(requirements, player().item(ItemID.COINS, 29).build());

        assertEquals(3, reasons.size(), reasons.toString());
        assertTrue(reasons.get(0).startsWith("needs 50 Agility"), reasons.toString());
        assertTrue(reasons.get(1).startsWith("needs quest "), reasons.toString());
        assertEquals("missing a required item (30 of [995])", reasons.get(2));
        assertTrue(met(requirements, maxedPlayer().completedQuest(Quest.DRAGON_SLAYER_I).item(ItemID.COINS, 30).build()));
    }

    @Test
    void combatAndTotalLevelAreChecked() {
        DefinitionRequirements combat = DefinitionRequirements.builder().combatLevel(100).build();
        DefinitionRequirements maxed = DefinitionRequirements.builder()
                .totalLevel(DefinitionRequirements.MAX_TOTAL_LEVEL).build();
        int highest = TransportRequirements.highestTotalLevel();

        assertFalse(met(combat, player().combatLevel(99).build()));
        assertTrue(met(combat, player().combatLevel(100).build()));
        assertEquals(0, highest % 99, "every skill counts 99 levels");
        assertTrue(highest >= 24 * 99 && highest <= Skill.values().length * 99, "the highest total is " + highest);
        assertFalse(met(maxed, player().totalLevel(highest - 1).build()));
        assertTrue(met(maxed, player().totalLevel(highest).build()));
    }

    @Test
    void anAlternativeWithItsOwnQuantityMeetsARequirement() {
        DefinitionRequirements shantayPass = DefinitionRequirements.builder()
                .item(RequiredItem.builder().anyOf(new int[]{1854}).quantity(1)
                        .alternatives(List.of(RequiredItem.of(ItemID.COINS, 5))).build())
                .build();

        assertTrue(met(shantayPass, player().item(1854, 1).build()));
        assertTrue(met(shantayPass, player().item(ItemID.COINS, 5).build()));
        assertFalse(met(shantayPass, player().item(ItemID.COINS, 4).build()));
    }

    @Test
    void aQuantityOfZeroMeansTheItemMustNotBeHeld() {
        DefinitionRequirements withoutMap = DefinitionRequirements.builder()
                .item(new RequiredItem(new int[]{2376}, 0)).build();

        assertTrue(met(withoutMap, player().build()));
        assertFalse(met(withoutMap, player().item(2376, 1).build()));
    }

    @Test
    void aStaffStandsInForItsRuneHoweverManyAreNeeded() {
        assertTrue(met(VARROCK_TELEPORT, player().item(AIR_RUNE, 3).item(FIRE_RUNE, 1).item(LAW_RUNE, 1).build()));
        assertTrue(met(VARROCK_TELEPORT, player().item(STAFF_OF_AIR, 1).item(FIRE_RUNE, 1).item(LAW_RUNE, 1).build()));
        assertFalse(met(VARROCK_TELEPORT, player().item(AIR_RUNE, 2).item(FIRE_RUNE, 1).item(LAW_RUNE, 1).build()));
        assertFalse(met(VARROCK_TELEPORT, player().item(STAFF_OF_AIR, 1).item(FIRE_RUNE, 1).build()),
                "nothing stands in for the law rune");
    }

    @Test
    void oneStaffHasToCoverEveryRuneThatIsMetByAStaff() {
        TransportRequirements.PlayerState twoStaves = player()
                .item(STAFF_OF_AIR, 1).item(STAFF_OF_FIRE, 1).item(LAW_RUNE, 1).build();
        TransportRequirements.PlayerState combinationStaff = player()
                .item(SMOKE_BATTLESTAFF, 1).item(LAW_RUNE, 1).build();

        assertFalse(met(VARROCK_TELEPORT, twoStaves), "only one staff can be wielded");
        assertTrue(met(VARROCK_TELEPORT, combinationStaff));
    }

    @Test
    void anOffhandCoversOneRuneBesideAStaff() {
        TransportRequirements.PlayerState staffAndTome = player()
                .item(STAFF_OF_AIR, 1).item(TOME_OF_FIRE, 1).item(LAW_RUNE, 1).build();

        assertTrue(met(VARROCK_TELEPORT, staffAndTome));
    }

    @Test
    void aVarbitCanWaiveAnItem() {
        DefinitionRequirements fairyRing = DefinitionRequirements.builder()
                .item(RequiredItem.builder().anyOf(new int[]{ItemID.DRAMEN_STAFF}).quantity(1)
                        .waivedByVarbit(new RequiredVar(FAIRY_RING_DIARY_VARBIT, Comparison.EQUAL, 1)).build())
                .build();

        assertFalse(met(fairyRing, player().build()));
        assertTrue(met(fairyRing, player().item(ItemID.DRAMEN_STAFF, 1).build()));
        assertTrue(met(fairyRing, player().varbit(FAIRY_RING_DIARY_VARBIT, 1).build()));
    }

    @Test
    void aFareAboveTheCurrencyThresholdIsRefusedEvenWhenItIsHeld() {
        DefinitionRequirements fare = DefinitionRequirements.builder().item(RequiredItem.of(ItemID.COINS, 3_200)).build();
        TransportRequirements.PlayerState rich = player().item(ItemID.COINS, 1_000_000).build();

        assertTrue(TransportRequirements.unmetReasons(fare, rich, 100_000).isEmpty());
        assertTrue(TransportRequirements.unmetReasons(fare, rich).isEmpty(), "no threshold means no limit");
        List<String> refused = TransportRequirements.unmetReasons(fare, rich, 1_000);
        assertEquals(1, refused.size());
        assertTrue(refused.get(0).contains("currency"), refused.toString());
    }

    @Test
    void varsAreComparedTheWayEachComparisonSays() {
        assertTrue(new RequiredVar(1, Comparison.BIT_SET, 3).matches(0b1000));
        assertFalse(new RequiredVar(1, Comparison.BIT_SET, 3).matches(0b0100));
        assertTrue(new RequiredVar(1, Comparison.ANY_BIT_OF, 0b0110).matches(0b0100));
        assertFalse(new RequiredVar(1, Comparison.ANY_BIT_OF, 0b0110).matches(0b1001));
        assertTrue(new RequiredVar(1, Comparison.AT_LEAST, 40).matches(40));
        assertFalse(new RequiredVar(1, Comparison.AT_MOST, 39).matches(40));
    }

    @Test
    void aCooldownPassesOnceMoreThanItsMinutesHaveGoneBy() {
        DefinitionRequirements homeTeleport = DefinitionRequirements.builder()
                .varPlayer(new RequiredVar(892, Comparison.COOLDOWN_MINUTES, 30)).build();
        long now = 30_000_000L;

        assertFalse(met(homeTeleport, player().epochMinutes(now).varPlayer(892, (int) now - 30).build()));
        assertTrue(met(homeTeleport, player().epochMinutes(now).varPlayer(892, (int) now - 31).build()));
        assertTrue(met(homeTeleport, player().epochMinutes(now).build()), "a teleport never used is not on cooldown");
    }

    @Test
    void aDatasetFairyRingNeedsAStaffOrTheLumbridgeEliteDiary() {
        TransportDefinition ring = datasetRow(row -> row.getCategory() == TransportCategory.FAIRY_RING
                && row.getRequirements().getQuests().isEmpty()
                && row.getRequirements().getItems().size() == 1);
        TransportRequirements.PlayerState.PlayerStateBuilder curedTheQueen = maxedPlayer().varbit(2326, 40);

        assertFalse(met(ring.getRequirements(), curedTheQueen.build()), ring.getId());
        assertTrue(met(ring.getRequirements(), maxedPlayer().varbit(2326, 40).item(ItemID.DRAMEN_STAFF, 1).build()));
        assertTrue(met(ring.getRequirements(),
                maxedPlayer().varbit(2326, 40).varbit(FAIRY_RING_DIARY_VARBIT, 1).build()));
        assertFalse(met(ring.getRequirements(), maxedPlayer().item(ItemID.DRAMEN_STAFF, 1).build()),
                "the rings do not work before Fairytale II has got far enough");
    }

    @Test
    void aDatasetSpellIsMetByRunesOrByAStaffInPlaceOfOne() {
        TransportDefinition varrock = datasetRow(row -> row.getCategory() == TransportCategory.TELEPORTATION_SPELL
                && "Varrock Teleport".equals(row.getDisplayInfo()));
        TransportRequirements.PlayerState.PlayerStateBuilder standardBook = maxedPlayer().varbit(4070, 0);

        assertFalse(met(varrock.getRequirements(), standardBook.build()));
        assertTrue(met(varrock.getRequirements(),
                maxedPlayer().varbit(4070, 0).item(AIR_RUNE, 3).item(FIRE_RUNE, 1).item(LAW_RUNE, 1).build()));
        assertTrue(met(varrock.getRequirements(),
                maxedPlayer().varbit(4070, 0).item(STAFF_OF_AIR, 1).item(FIRE_RUNE, 1).item(LAW_RUNE, 1).build()));
        assertFalse(met(varrock.getRequirements(),
                player().varbit(4070, 0).item(AIR_RUNE, 3).item(FIRE_RUNE, 1).item(LAW_RUNE, 1).build()),
                "the spell needs its magic level");
    }

    @Test
    void aDatasetItemTeleportRequiresOneOfItsItems() {
        TransportDefinition glory = datasetRow(row -> row.getCategory() == TransportCategory.TELEPORTATION_ITEM
                && row.getId().contains("amulet-of-glory-edgeville"));

        assertFalse(met(glory.getRequirements(), maxedPlayer().build()));
        assertTrue(met(glory.getRequirements(), maxedPlayer().item(glory.getItemIds().get(0), 1).build()));
        assertEquals(30, glory.getMaxWildernessLevel());
    }
}
