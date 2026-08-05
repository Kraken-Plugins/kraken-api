package plugins.api.requirements;

import com.kraken.api.service.magic.spellbook.Spellbook;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import plugins.api.world.Facility;
import plugins.api.world.NamedLocation;

import java.util.List;
import java.util.Set;

/**
 * The world state a test needs before it runs, declared rather than established by hand.
 *
 * <p>Every test used to open with its own setup block, and five of them contained the same fourteen
 * lines of bank booth clicking in five slightly different forms. Worse, the requirements that could
 * not be expressed as code — which bank items, which skill levels, which location — lived only in
 * {@code docs/TESTS.md}, where nothing enforced them. Declaring the state instead lets one engine
 * establish it, and lets an unmet requirement be reported as a skip with a reason rather than as a
 * failure that looks identical to a real regression.</p>
 *
 * <p>Instances are immutable and must be cheap to build: {@code requirements()} is called off the
 * client thread, possibly before login, and its result is cached. Never read game state here.</p>
 */
@Value
@Builder(toBuilder = true)
public class TestRequirements {

    /** Declares no requirements at all. The default for any test that has not been migrated. */
    public static final TestRequirements NONE = TestRequirements.builder().build();

    /** A specific place the test must run. Null means "anywhere satisfying {@link #facilities}". */
    NamedLocation location;

    /**
     * Capabilities the location must offer. Preferred over {@link #location}: the runner can satisfy
     * these without travelling if the player already happens to be somewhere suitable.
     */
    @Singular("facility")
    Set<Facility> facilities;

    /**
     * Where item setup happens when the run location cannot provide it.
     *
     * <p>Needed whenever a test runs somewhere with no bank — at a deposit box, for instance. Without
     * a staging concept those tests cannot be automated at all, because there is nowhere to withdraw
     * their items from.</p>
     */
    NamedLocation stagingLocation;

    /** Tiles of slack accepted around the location anchor. Negative uses the location's own radius. */
    @Builder.Default
    int locationRadius = -1;

    /** The bank interface state required on entry. */
    @Builder.Default
    BankState bankState = BankState.ANY;

    /** The deposit box interface state required on entry. */
    @Builder.Default
    DepositBoxState depositBoxState = DepositBoxState.ANY;

    /** How aggressively to reshape the inventory and equipment. */
    @Builder.Default
    InventoryPolicy inventoryPolicy = InventoryPolicy.AUTO;

    /** Items that must be carried when the test starts. */
    @Singular("inventoryItem")
    List<ItemRequirement> inventoryItems;

    /** Items that must be worn when the test starts. */
    @Singular("equippedItem")
    List<ItemRequirement> equippedItems;

    /**
     * Items that must <em>not</em> be carried. The inventory test, for example, asserts that a query
     * finds nothing, which only means anything if the item is genuinely absent.
     */
    @Singular("forbiddenItem")
    List<ItemRequirement> forbiddenItems;

    /**
     * Items that must exist in the bank but are not withdrawn. Checked <em>before</em> the player is
     * stripped, so a missing item cannot leave the account naked for the rest of the run.
     */
    @Singular("bankStock")
    List<ItemRequirement> bankStock;

    /** Whether the test needs any droppable item, without caring which. */
    @Builder.Default
    boolean requiresDroppableItem = false;

    /** NPCs that must be in the scene. Verifiable only; NPCs cannot be summoned. */
    @Singular("nearbyNpc")
    List<NpcRequirement> nearbyNpcs;

    /** Minimum skill levels. Verifiable only. */
    @Singular("skill")
    List<SkillRequirement> skills;

    /** The spellbook the player must be on. Null means any. */
    Spellbook spellbook;

    /** Checks the runner can verify but not establish. */
    @Singular("customCheck")
    List<CustomRequirement> customChecks;

    /** A destination tile to publish before the test runs, replacing a manual shift click. */
    TargetTile targetTile;

    /** What this test does to the world that outlives it. Advisory; see {@link SideEffect}. */
    @Singular("sideEffect")
    Set<SideEffect> sideEffects;

    /** Ordering nudge within a location group. Higher sorts later. */
    @Builder.Default
    int orderHint = 0;

    /**
     * Whether the test is excluded from bulk runs by default.
     *
     * <p>Covers two cases: tests that spend real resources, such as the Grand Exchange test placing
     * live orders, and tests whose side effects are not worth recovering from in a sequence, such as
     * the world query test hopping worlds.</p>
     */
    @Builder.Default
    boolean destructive = false;

    /** Overrides the global per-test timeout in milliseconds. Zero uses the configured default. */
    @Builder.Default
    long timeoutMs = 0;

    /**
     * Resolves {@link InventoryPolicy#AUTO} against what this test actually declared.
     *
     * <p>A test declaring nothing item shaped resolves to {@link InventoryPolicy#NO_CHANGE}, so it is
     * never sent to a bank to empty an inventory it does not care about.</p>
     *
     * @return the effective policy, never {@link InventoryPolicy#AUTO}
     */
    public InventoryPolicy resolveInventoryPolicy() {
        if (inventoryPolicy != InventoryPolicy.AUTO) {
            return inventoryPolicy;
        }

        boolean touchesItems = !inventoryItems.isEmpty()
                || !equippedItems.isEmpty()
                || !forbiddenItems.isEmpty()
                || requiresDroppableItem;

        return touchesItems ? InventoryPolicy.EXACT : InventoryPolicy.NO_CHANGE;
    }

    /**
     * Reports whether this test declared a given side effect.
     *
     * @param effect the side effect to look for
     * @return true when it was declared
     */
    public boolean hasSideEffect(SideEffect effect) {
        return sideEffects.contains(effect);
    }

    /**
     * Reports whether satisfying this test requires being somewhere in particular.
     *
     * @return true when either an explicit location or any facility was declared
     */
    public boolean hasLocationConstraint() {
        return location != null || !facilities.isEmpty();
    }

    /**
     * Reports whether the runner needs a bank to satisfy these requirements.
     *
     * @return true when items must be withdrawn or banked, or the bank must be left open
     */
    public boolean needsBankAccess() {
        if (bankState == BankState.OPEN || !bankStock.isEmpty()) {
            return true;
        }

        InventoryPolicy effective = resolveInventoryPolicy();
        return effective == InventoryPolicy.EXACT || effective == InventoryPolicy.TOP_UP;
    }
}
