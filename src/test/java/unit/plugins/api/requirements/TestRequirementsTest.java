package unit.plugins.api.requirements;

import org.junit.jupiter.api.Test;
import plugins.api.requirements.BankState;
import plugins.api.requirements.InventoryPolicy;
import plugins.api.requirements.ItemRequirement;
import plugins.api.requirements.SideEffect;
import plugins.api.requirements.TestRequirements;
import plugins.api.world.Facility;
import plugins.api.world.NamedLocation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the derived logic on {@link TestRequirements}. These are the decisions that determine how
 * much work the runner does for a given test, so getting them wrong means either pointless bank trips
 * or a test starting against a world it did not ask for.
 */
class TestRequirementsTest {

    @Test
    void noneDeclaresNothing() {
        TestRequirements none = TestRequirements.NONE;

        assertTrue(none.getFacilities().isEmpty());
        assertTrue(none.getInventoryItems().isEmpty());
        assertTrue(none.getSideEffects().isEmpty());
        assertEquals(BankState.ANY, none.getBankState());
        assertFalse(none.hasLocationConstraint());
        assertFalse(none.needsBankAccess());
    }

    @Test
    void autoPolicyLeavesInventoryAloneWhenNoItemsAreDeclared() {
        // The important half of AUTO: a read-only test must never trigger a bank trip just to empty an
        // inventory it does not care about.
        assertEquals(InventoryPolicy.NO_CHANGE, TestRequirements.NONE.resolveInventoryPolicy());
    }

    @Test
    void autoPolicyBecomesExactWhenInventoryItemsAreDeclared() {
        TestRequirements requirements = TestRequirements.builder()
                .inventoryItem(ItemRequirement.of("Chisel"))
                .build();

        assertEquals(InventoryPolicy.EXACT, requirements.resolveInventoryPolicy());
    }

    @Test
    void autoPolicyBecomesExactForForbiddenItemsAlone() {
        // A negative assertion is only meaningful if the item is genuinely absent, so a forbidden item
        // has to be enough on its own to make the runner reshape the inventory.
        TestRequirements requirements = TestRequirements.builder()
                .forbiddenItem(ItemRequirement.of("Gold bar"))
                .build();

        assertEquals(InventoryPolicy.EXACT, requirements.resolveInventoryPolicy());
    }

    @Test
    void autoPolicyBecomesExactForADroppableItemRequirement() {
        TestRequirements requirements = TestRequirements.builder()
                .requiresDroppableItem(true)
                .build();

        assertEquals(InventoryPolicy.EXACT, requirements.resolveInventoryPolicy());
    }

    @Test
    void anExplicitPolicyIsNeverOverridden() {
        TestRequirements requirements = TestRequirements.builder()
                .inventoryItem(ItemRequirement.of("Chisel"))
                .inventoryPolicy(InventoryPolicy.NO_CHANGE)
                .build();

        assertEquals(InventoryPolicy.NO_CHANGE, requirements.resolveInventoryPolicy());
    }

    @Test
    void bankStockAloneRequiresBankAccessWithoutReshapingTheInventory() {
        // Bank stock is verified, not withdrawn, so it needs the bank but must not imply EXACT.
        TestRequirements requirements = TestRequirements.builder()
                .bankStock(ItemRequirement.of("Rune platebody"))
                .build();

        assertTrue(requirements.needsBankAccess());
        assertEquals(InventoryPolicy.NO_CHANGE, requirements.resolveInventoryPolicy());
    }

    @Test
    void anOpenBankRequirementNeedsBankAccess() {
        TestRequirements requirements = TestRequirements.builder()
                .bankState(BankState.OPEN)
                .build();

        assertTrue(requirements.needsBankAccess());
    }

    @Test
    void aClosedBankRequirementDoesNotNeedBankAccessOnItsOwn() {
        TestRequirements requirements = TestRequirements.builder()
                .bankState(BankState.CLOSED)
                .build();

        assertFalse(requirements.needsBankAccess());
    }

    @Test
    void facilitiesCountAsALocationConstraint() {
        TestRequirements requirements = TestRequirements.builder()
                .facility(Facility.BANK_BOOTH)
                .build();

        assertTrue(requirements.hasLocationConstraint());
    }

    @Test
    void anExplicitLocationCountsAsALocationConstraint() {
        TestRequirements requirements = TestRequirements.builder()
                .location(NamedLocation.GRAND_EXCHANGE)
                .build();

        assertTrue(requirements.hasLocationConstraint());
    }

    @Test
    void sideEffectsAreQueryable() {
        TestRequirements requirements = TestRequirements.builder()
                .sideEffect(SideEffect.TELEPORTS)
                .sideEffect(SideEffect.CONSUMES_ITEMS)
                .build();

        assertTrue(requirements.hasSideEffect(SideEffect.TELEPORTS));
        assertFalse(requirements.hasSideEffect(SideEffect.HOPS_WORLDS));
    }

    @Test
    void toBuilderPreservesEverythingItDidNotChange() {
        TestRequirements original = TestRequirements.builder()
                .facility(Facility.BANK_BOOTH)
                .bankStock(ItemRequirement.of("Lobster", 5))
                .sideEffect(SideEffect.DROPS_ITEMS)
                .build();

        TestRequirements derived = original.toBuilder().bankState(BankState.OPEN).build();

        assertEquals(BankState.OPEN, derived.getBankState());
        assertEquals(original.getFacilities(), derived.getFacilities());
        assertEquals(original.getBankStock(), derived.getBankStock());
        assertEquals(original.getSideEffects(), derived.getSideEffects());
    }

    @Test
    void noneIsASharedImmutableInstance() {
        assertSame(TestRequirements.NONE, TestRequirements.NONE);
    }
}
