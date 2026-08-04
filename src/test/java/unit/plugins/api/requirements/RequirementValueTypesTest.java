package unit.plugins.api.requirements;

import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import org.junit.jupiter.api.Test;
import plugins.api.requirements.CustomRequirement;
import plugins.api.requirements.ItemRequirement;
import plugins.api.requirements.NpcRequirement;
import plugins.api.requirements.SkillRequirement;
import plugins.api.requirements.TargetTile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the individual requirement value types: how they are built, and how they render themselves
 * into the reasons a skipped test reports.
 */
class RequirementValueTypesTest {

    // ---------- ItemRequirement ----------

    @Test
    void itemsDefaultToOneUnnoted() {
        ItemRequirement lobster = ItemRequirement.of("Lobster");

        assertEquals(1, lobster.getQuantity());
        assertEquals(ItemRequirement.Noted.UNNOTED, lobster.getNoted());
        assertFalse(lobster.byId());
    }

    @Test
    void itemsCanBeIdentifiedById() {
        ItemRequirement swordfish = ItemRequirement.of(373, 4);

        assertTrue(swordfish.byId());
        assertEquals(373, swordfish.getId());
        assertEquals(4, swordfish.getQuantity());
    }

    @Test
    void aDefaultItemRequirementIsNotTreatedAsIdBased() {
        // id defaults to -1, so a name-based requirement must not accidentally look like item -1.
        assertFalse(ItemRequirement.of("Chisel").byId());
    }

    @Test
    void itemDescriptionsReadNaturally() {
        assertEquals("Lobster", ItemRequirement.of("Lobster").describe());
        assertEquals("5x Lobster", ItemRequirement.of("Lobster", 5).describe());
        assertEquals("item 1623", ItemRequirement.of(1623, 1).describe());
        assertEquals("10x Swordfish (noted)", ItemRequirement.noted("Swordfish", 10).describe());
    }

    // ---------- SkillRequirement ----------

    @Test
    void skillsDisallowBoostsByDefault() {
        // The dps test compares readings taken seconds apart, so a boost draining mid run would look
        // exactly like a regression.
        assertFalse(SkillRequirement.of(Skill.ATTACK, 40).isAllowBoosted());
        assertTrue(SkillRequirement.boostable(Skill.ATTACK, 40).isAllowBoosted());
    }

    @Test
    void skillDescriptionsNameTheSkillAndLevel() {
        String described = SkillRequirement.of(Skill.CRAFTING, 20).describe();

        assertTrue(described.contains("20"));
        assertTrue(described.toLowerCase().contains("craft"));
    }

    // ---------- NpcRequirement ----------

    @Test
    void npcRequirementsAcceptAnyOfSeveralNames() {
        NpcRequirement target = NpcRequirement.anyOf("Guard", "Man", "Goblin");

        assertEquals(3, target.getAnyOfNames().size());
        assertTrue(target.isNameContains());
        assertEquals(15, target.getWithinTiles());
    }

    @Test
    void npcRangeCanBeTightened() {
        assertEquals(10, NpcRequirement.within("Banker", 10).getWithinTiles());
    }

    @Test
    void npcDescriptionsListEveryAcceptableName() {
        String described = NpcRequirement.anyOf("Guard", "Man").describe();

        assertTrue(described.contains("Guard"));
        assertTrue(described.contains("Man"));
    }

    // ---------- CustomRequirement ----------

    @Test
    void customChecksReportTheirResult() {
        assertTrue(CustomRequirement.of("always true", ctx -> true).isSatisfied(null));
        assertFalse(CustomRequirement.of("always false", ctx -> false).isSatisfied(null));
    }

    @Test
    void aThrowingCustomCheckFailsClosed() {
        // A check that blows up has not established anything, so it must not be read as a pass.
        CustomRequirement broken = CustomRequirement.of("explodes", ctx -> {
            throw new IllegalStateException("boom");
        });

        assertFalse(broken.isSatisfied(null));
    }

    // ---------- TargetTile ----------

    @Test
    void relativeTilesResolveAgainstThePlayer() {
        WorldPoint player = new WorldPoint(3253, 3421, 0);

        WorldPoint resolved = TargetTile.relativeToPlayer(6, -3).resolve(player);

        assertEquals(3259, resolved.getX());
        assertEquals(3418, resolved.getY());
        assertEquals(0, resolved.getPlane());
    }

    @Test
    void absoluteTilesIgnoreThePlayer() {
        WorldPoint fixed = new WorldPoint(3165, 3487, 0);

        assertEquals(fixed, TargetTile.absolute(fixed).resolve(new WorldPoint(3253, 3421, 0)));
        assertEquals(fixed, TargetTile.absolute(fixed).resolve(null));
    }

    @Test
    void aRelativeTileWithoutAPlayerResolvesToNothing() {
        assertNull(TargetTile.relativeToPlayer(6, 6).resolve(null));
    }
}
