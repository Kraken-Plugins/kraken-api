package plugins.api.precondition;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.query.container.ContainerItem;
import com.kraken.api.query.container.bank.BankEntity;
import com.kraken.api.query.container.bank.BankInventoryEntity;
import com.kraken.api.query.container.inventory.InventoryEntity;
import com.kraken.api.query.equipment.EquipmentEntity;
import com.kraken.api.query.npc.NpcQuery;
import com.kraken.api.service.bank.BankService;
import com.kraken.api.service.magic.spellbook.Spellbook;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GameState;
import net.runelite.api.coords.WorldPoint;
import plugins.api.TargetTileProvider;
import plugins.api.requirements.*;
import plugins.api.suite.CancellationToken;
import plugins.api.suite.TestCancelledException;
import plugins.api.world.Facility;
import plugins.api.world.NamedLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Drives the game world into the state a test declared, so tests do not have to set themselves up.
 *
 * <p>Phases run cheapest-first and unfixable-first. Skills and spellbook are checked before anything
 * moves, because no amount of walking or banking will raise a level and there is no point spending
 * ninety seconds travelling to discover that. Items are established before travel, because the bank
 * that supplies them is usually not where the test runs.</p>
 *
 * <p>The single most important ordering rule is inside item setup: <b>bank stock is verified before
 * the player is stripped</b>. Depositing everything first and only then discovering the required gear
 * is missing would leave the account naked for the remainder of the run.</p>
 *
 * <p>Nothing here trusts a service's return value as proof of success. {@code BankService.depositAll}
 * returns true when the inventory was already empty and false when the bank is shut, so it reports
 * what it attempted rather than what happened; every phase re-reads the world instead.</p>
 *
 * <p>Everything here blocks and must be called off the client thread.</p>
 */
@Slf4j
@Singleton
public class PreconditionEngine {

    private static final long CONTAINER_TIMEOUT_MS = 5000;
    private static final long EQUIP_TIMEOUT_MS = 5000;

    @Inject
    private Context ctx;

    @Inject
    private BankService bankService;

    @Inject
    private BankHelper bankHelper;

    @Inject
    private SuiteWalker walker;

    @Inject
    private Waiter waiter;

    @Inject
    private TargetTileProvider targetTiles;

    /**
     * Puts the world into the declared state.
     *
     * @param requirements what the test needs; {@link TestRequirements#NONE} is a fast no-op
     * @param token polled throughout so a cancelled run stops promptly
     * @param onPhase receives short progress descriptions such as "Travelling to Grand Exchange";
     *                may be null
     * @return the outcome, carrying a human readable reason when the state could not be reached
     */
    public PreconditionResult satisfy(TestRequirements requirements, CancellationToken token,
                                      Consumer<String> onPhase) {
        List<String> steps = new ArrayList<>();

        try {
            if (requirements == null) {
                return PreconditionResult.satisfied(steps);
            }

            if (!isLoggedIn()) {
                return PreconditionResult.unsatisfiable("not logged in", steps);
            }

            // Unfixable checks first: never spend a two minute walk to discover a level is too low.
            PreconditionResult characterCheck = checkCharacter(requirements, steps);
            if (characterCheck != null) {
                return characterCheck;
            }

            PreconditionResult itemSetup = establishItems(requirements, token, onPhase, steps);
            if (itemSetup != null) {
                return itemSetup;
            }

            PreconditionResult travel = travelToRunLocation(requirements, token, onPhase, steps);
            if (travel != null) {
                return travel;
            }

            PreconditionResult interfaces = establishInterfaceState(requirements, token, onPhase, steps);
            if (interfaces != null) {
                return interfaces;
            }

            PreconditionResult world = checkWorldState(requirements, steps);
            if (world != null) {
                return world;
            }

            publishTargetTile(requirements, steps);

            return PreconditionResult.satisfied(steps);

        } catch (TestCancelledException e) {
            return PreconditionResult.cancelled(steps);
        } catch (Exception e) {
            log.error("Unexpected error while establishing preconditions", e);
            return PreconditionResult.setupFailed("setup threw " + e.getClass().getSimpleName()
                    + ": " + e.getMessage(), steps);
        }
    }

    /**
     * Re-checks the requirements without changing anything.
     *
     * <p>Used as a final gate after setup and by the panel to preview whether a test could run right
     * now. Only covers the conditions that are observable without mutating the world.</p>
     *
     * @param requirements what the test needs
     * @return the outcome
     */
    public PreconditionResult verify(TestRequirements requirements) {
        List<String> steps = new ArrayList<>();

        if (requirements == null) {
            return PreconditionResult.satisfied(steps);
        }

        if (!isLoggedIn()) {
            return PreconditionResult.unsatisfiable("not logged in", steps);
        }

        PreconditionResult characterCheck = checkCharacter(requirements, steps);
        if (characterCheck != null) {
            return characterCheck;
        }

        PreconditionResult world = checkWorldState(requirements, steps);
        if (world != null) {
            return world;
        }

        return PreconditionResult.satisfied(steps);
    }

    /**
     * Checks the things about the character that cannot be changed by banking or walking.
     *
     * @param requirements what the test needs
     * @param steps accumulating audit trail
     * @return a failing result, or null when every check passed
     */
    private PreconditionResult checkCharacter(TestRequirements requirements, List<String> steps) {
        for (SkillRequirement skill : requirements.getSkills()) {
            int level = ctx.players().local().getLevel(skill.getSkill());
            if (level < skill.getLevel()) {
                return PreconditionResult.unsatisfiable(
                        "requires " + skill.describe() + " but the level is " + level, steps);
            }

            if (!skill.isAllowBoosted()) {
                int boosted = ctx.players().local().getBoostedLevel(skill.getSkill());
                if (boosted != level) {
                    // The dps test compares readings taken seconds apart; a boost draining between them
                    // looks exactly like the gear change it is trying to measure.
                    return PreconditionResult.unsatisfiable(
                            skill.getSkill().getName() + " is boosted (" + boosted + " vs " + level
                                    + ") and this test needs stable levels", steps);
                }
            }
        }

        if (requirements.getSpellbook() != null) {
            Spellbook current = Spellbook.getCurrentSpellbook();
            if (current != requirements.getSpellbook()) {
                return PreconditionResult.unsatisfiable(
                        "requires the " + requirements.getSpellbook() + " spellbook but you are on "
                                + current, steps);
            }
        }

        if (!requirements.getSkills().isEmpty() || requirements.getSpellbook() != null) {
            steps.add("Verified character requirements");
        }

        return null;
    }

    /**
     * Establishes the inventory and equipment the test asked for.
     *
     * @param requirements what the test needs
     * @param token cancellation token
     * @param onPhase progress sink
     * @param steps accumulating audit trail
     * @return a failing result, or null when the items are in place
     */
    private PreconditionResult establishItems(TestRequirements requirements, CancellationToken token,
                                              Consumer<String> onPhase, List<String> steps) {
        InventoryPolicy policy = requirements.resolveInventoryPolicy();

        if (policy == InventoryPolicy.NO_CHANGE && requirements.getBankStock().isEmpty()) {
            return null;
        }

        if (!requirements.needsBankAccess() && policy == InventoryPolicy.NO_CHANGE) {
            return null;
        }

        // Fast path: if the world already looks right, skip the whole bank trip. This is what keeps a
        // sequence of similar tests from re-banking between every one of them.
        if (policy != InventoryPolicy.EXACT && itemsAlreadySatisfied(requirements)) {
            steps.add("Items already in place");
            return null;
        }

        NamedLocation staging = resolveStagingLocation(requirements);
        if (staging != null) {
            report(onPhase, "Travelling to " + staging.getDisplayName());
            if (!walker.walkTo(staging, token)) {
                return PreconditionResult.setupFailed(
                        "could not travel to " + staging.getDisplayName() + " to collect items", steps);
            }
            steps.add("Travelled to " + staging.getDisplayName());
        }

        report(onPhase, "Opening the bank");
        if (!bankHelper.open(token)) {
            return PreconditionResult.setupFailed(
                    "could not open the bank to set up items", steps);
        }
        steps.add("Opened the bank");

        // Verified BEFORE stripping the player. Discovering a missing item after depositing everything
        // would leave the account naked for the rest of the run.
        for (ItemRequirement stock : requirements.getBankStock()) {
            if (bankQuantity(stock) < stock.getQuantity()) {
                return PreconditionResult.unsatisfiable(
                        stock.describe() + " is not in the bank", steps);
            }
        }

        for (ItemRequirement needed : requirements.getInventoryItems()) {
            if (bankQuantity(needed) < needed.getQuantity() && inventoryQuantity(needed) < needed.getQuantity()) {
                return PreconditionResult.unsatisfiable(
                        needed.describe() + " is neither carried nor in the bank", steps);
            }
        }

        if (!requirements.getBankStock().isEmpty()) {
            steps.add("Verified bank stock");
        }

        PreconditionResult cleared = clearContainers(requirements, policy, token, onPhase, steps);
        if (cleared != null) {
            return cleared;
        }

        return withdrawDeclaredItems(requirements, token, onPhase, steps);
    }

    /**
     * Empties the inventory and equipment, or removes just the forbidden items.
     *
     * @param requirements what the test needs
     * @param policy the resolved inventory policy
     * @param token cancellation token
     * @param onPhase progress sink
     * @param steps accumulating audit trail
     * @return a failing result, or null on success
     */
    private PreconditionResult clearContainers(TestRequirements requirements, InventoryPolicy policy,
                                               CancellationToken token, Consumer<String> onPhase,
                                               List<String> steps) {
        if (policy == InventoryPolicy.EXACT) {
            report(onPhase, "Banking everything carried and worn");

            bankService.depositAll();
            if (!waiter.until(() -> ctx.inventory().isEmpty(), CONTAINER_TIMEOUT_MS, token,
                    "the inventory to empty")) {
                return PreconditionResult.setupFailed("could not empty the inventory", steps);
            }

            bankService.depositAllEquipment();
            if (!waiter.until(() -> ctx.equipment().inInterface().list().isEmpty(), CONTAINER_TIMEOUT_MS,
                    token, "worn equipment to be banked")) {
                return PreconditionResult.setupFailed("could not bank worn equipment", steps);
            }

            steps.add("Emptied inventory and equipment");
            return null;
        }

        for (ItemRequirement forbidden : requirements.getForbiddenItems()) {
            if (inventoryQuantity(forbidden) <= 0) {
                continue;
            }

            token.throwIfCancelled("banking a forbidden item");
            BankInventoryEntity carried = findInBankInventory(forbidden);
            if (carried != null) {
                carried.depositAll();
                waiter.until(() -> inventoryQuantity(forbidden) <= 0, CONTAINER_TIMEOUT_MS, token,
                        forbidden.describe() + " to be banked");
            }

            if (inventoryQuantity(forbidden) > 0) {
                return PreconditionResult.setupFailed(
                        "could not bank " + forbidden.describe() + ", which this test forbids", steps);
            }

            steps.add("Banked forbidden " + forbidden.describe());
        }

        return null;
    }

    /**
     * Withdraws and equips everything the test declared.
     *
     * @param requirements what the test needs
     * @param token cancellation token
     * @param onPhase progress sink
     * @param steps accumulating audit trail
     * @return a failing result, or null on success
     */
    private PreconditionResult withdrawDeclaredItems(TestRequirements requirements, CancellationToken token,
                                                     Consumer<String> onPhase, List<String> steps) {
        List<ItemRequirement> wanted = new ArrayList<>(requirements.getInventoryItems());

        // Gear the player is already wearing needs no withdrawal. Without this check its carried count
        // reads as zero and a second copy is pulled out of the bank for no reason.
        for (ItemRequirement item : requirements.getEquippedItems()) {
            if (!isWearing(item)) {
                wanted.add(item);
            }
        }

        if (wanted.isEmpty()) {
            return null;
        }

        report(onPhase, "Withdrawing items");
        bankService.setWithdrawMode(false);

        for (ItemRequirement item : wanted) {
            token.throwIfCancelled("withdrawing " + item.describe());

            int shortfall = item.getQuantity() - inventoryQuantity(item);
            if (shortfall <= 0) {
                continue;
            }

            if (!withdraw(item, shortfall, token)) {
                return PreconditionResult.setupFailed(
                        "could not withdraw " + item.describe(), steps);
            }

            steps.add("Withdrew " + item.describe());
        }

        for (ItemRequirement item : requirements.getEquippedItems()) {
            token.throwIfCancelled("equipping " + item.describe());

            if (isWearing(item)) {
                continue;
            }

            // Equipping goes through the equipment query's inventory view rather than the plain
            // inventory query: only EquipmentEntity resolves the wield/wear widget action.
            EquipmentEntity carried = findEquippableInInventory(item);
            if (carried == null || !carried.wieldOrWear()) {
                return PreconditionResult.setupFailed("could not equip " + item.describe(), steps);
            }

            if (!waiter.until(() -> isWearing(item), EQUIP_TIMEOUT_MS, token,
                    item.describe() + " to be equipped")) {
                return PreconditionResult.setupFailed(
                        "equipped " + item.describe() + " but it is not worn", steps);
            }

            steps.add("Equipped " + item.describe());
        }

        return null;
    }

    /**
     * Walks to wherever the test needs to run.
     *
     * @param requirements what the test needs
     * @param token cancellation token
     * @param onPhase progress sink
     * @param steps accumulating audit trail
     * @return a failing result, or null when the player is in position
     */
    private PreconditionResult travelToRunLocation(TestRequirements requirements, CancellationToken token,
                                                   Consumer<String> onPhase, List<String> steps) {
        NamedLocation destination = resolveRunLocation(requirements);
        if (destination == null || destination == NamedLocation.ANYWHERE) {
            return null;
        }

        WorldPoint here = ctx.players().local().location();
        if (here != null && destination.contains(here)) {
            return null;
        }

        report(onPhase, "Travelling to " + destination.getDisplayName());
        if (!walker.walkTo(destination, token)) {
            return PreconditionResult.setupFailed(
                    "could not travel to " + destination.getDisplayName(), steps);
        }

        steps.add("Travelled to " + destination.getDisplayName());
        return null;
    }

    /**
     * Opens or closes the banking interfaces to match what was declared.
     *
     * @param requirements what the test needs
     * @param token cancellation token
     * @param onPhase progress sink
     * @param steps accumulating audit trail
     * @return a failing result, or null on success
     */
    private PreconditionResult establishInterfaceState(TestRequirements requirements, CancellationToken token,
                                                       Consumer<String> onPhase, List<String> steps) {
        if (requirements.getBankState() == BankState.OPEN) {
            report(onPhase, "Opening the bank");
            if (!bankHelper.open(token)) {
                return PreconditionResult.setupFailed("could not open the bank", steps);
            }
            steps.add("Bank open");
        } else if (requirements.getBankState() == BankState.CLOSED) {
            if (!bankHelper.close(token)) {
                return PreconditionResult.setupFailed("could not close the bank", steps);
            }
            steps.add("Bank closed");
        }

        if (requirements.getDepositBoxState() == DepositBoxState.OPEN) {
            report(onPhase, "Opening the deposit box");
            if (!bankHelper.openDepositBox(token)) {
                return PreconditionResult.setupFailed("could not open the deposit box", steps);
            }
            steps.add("Deposit box open");
        } else if (requirements.getDepositBoxState() == DepositBoxState.CLOSED) {
            if (!bankHelper.closeDepositBox(token)) {
                return PreconditionResult.setupFailed("could not close the deposit box", steps);
            }
            steps.add("Deposit box closed");
        }

        return null;
    }

    /**
     * Checks the parts of the world the engine can observe but not create.
     *
     * @param requirements what the test needs
     * @param steps accumulating audit trail
     * @return a failing result, or null when every check passed
     */
    private PreconditionResult checkWorldState(TestRequirements requirements, List<String> steps) {
        for (NpcRequirement npc : requirements.getNearbyNpcs()) {
            if (!isNpcPresent(npc)) {
                return PreconditionResult.unsatisfiable("needs " + npc.describe(), steps);
            }
        }

        if (requirements.isRequiresDroppableItem() && findDroppableItem() == null) {
            return PreconditionResult.unsatisfiable(
                    "needs at least one droppable item in the inventory", steps);
        }

        for (CustomRequirement custom : requirements.getCustomChecks()) {
            if (!custom.isSatisfied(ctx)) {
                return PreconditionResult.unsatisfiable("needs " + custom.getDescription(), steps);
            }
        }

        return null;
    }

    /**
     * Publishes the declared target tile so tests that poll for a manually picked tile return at once.
     *
     * @param requirements what the test needs
     * @param steps accumulating audit trail
     */
    private void publishTargetTile(TestRequirements requirements, List<String> steps) {
        if (requirements.getTargetTile() == null) {
            return;
        }

        WorldPoint resolved = requirements.getTargetTile().resolve(ctx.players().local().location());
        if (resolved == null) {
            return;
        }

        targetTiles.setSuiteTile(resolved);
        steps.add("Published target tile " + resolved);
    }

    /**
     * Chooses where item setup should happen.
     *
     * @param requirements what the test needs
     * @return the staging location, or null when no bank trip is needed
     */
    private NamedLocation resolveStagingLocation(TestRequirements requirements) {
        if (requirements.getStagingLocation() != null) {
            return requirements.getStagingLocation();
        }

        // Already somewhere with a bank: no need to move at all.
        WorldPoint here = ctx.players().local().location();
        NamedLocation current = NamedLocation.at(here).orElse(null);
        if (current != null && current.getFacilities().contains(Facility.BANK_BOOTH)) {
            return null;
        }

        if (bankService.isOpen()) {
            return null;
        }

        NamedLocation runLocation = resolveRunLocation(requirements);
        if (runLocation != null && runLocation.getFacilities().contains(Facility.BANK_BOOTH)) {
            return runLocation;
        }

        List<NamedLocation> banks = NamedLocation.providing(
                java.util.EnumSet.of(Facility.BANK_BOOTH), here);
        return banks.isEmpty() ? null : banks.get(0);
    }

    /**
     * Chooses where the test itself should run.
     *
     * @param requirements what the test needs
     * @return the run location, or null when the test works anywhere
     */
    private NamedLocation resolveRunLocation(TestRequirements requirements) {
        if (requirements.getLocation() != null) {
            return requirements.getLocation();
        }

        if (requirements.getFacilities().isEmpty()) {
            return null;
        }

        WorldPoint here = ctx.players().local().location();

        // Prefer staying put: if the current location already offers everything, do not travel.
        NamedLocation current = NamedLocation.at(here).orElse(null);
        if (current != null && current.provides(requirements.getFacilities())) {
            return current;
        }

        List<NamedLocation> candidates = NamedLocation.providing(requirements.getFacilities(), here);
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    /**
     * Reports whether the declared items are already carried and worn.
     *
     * @param requirements what the test needs
     * @return true when nothing needs withdrawing or banking
     */
    private boolean itemsAlreadySatisfied(TestRequirements requirements) {
        for (ItemRequirement item : requirements.getInventoryItems()) {
            if (inventoryQuantity(item) < item.getQuantity()) {
                return false;
            }
        }

        for (ItemRequirement item : requirements.getEquippedItems()) {
            if (!isWearing(item)) {
                return false;
            }
        }

        for (ItemRequirement forbidden : requirements.getForbiddenItems()) {
            if (inventoryQuantity(forbidden) > 0) {
                return false;
            }
        }

        return !requirements.isRequiresDroppableItem() || findDroppableItem() != null;
    }

    /**
     * Withdraws a quantity of an item and confirms it arrived.
     *
     * <p>A fresh bank query is built for every withdrawal on purpose: {@code BankQuery} returns an
     * empty stream when the same instance is queried twice within one game tick, and a cached
     * {@code BankEntity} holds a slot index that goes stale as soon as anything is withdrawn.</p>
     *
     * @param item what to withdraw
     * @param amount how many more are needed
     * @param token cancellation token
     * @return true when the inventory count rose by the requested amount
     */
    private boolean withdraw(ItemRequirement item, int amount, CancellationToken token) {
        BankEntity entity = findInBank(item);
        if (entity == null) {
            return false;
        }

        int before = inventoryQuantity(item);
        boolean noted = item.getNoted() == ItemRequirement.Noted.NOTED;

        if (amount == 1 && !noted) {
            entity.withdrawOne();
        } else {
            entity.withdraw(amount, noted);
        }

        return waiter.until(() -> inventoryQuantity(item) >= before + amount, CONTAINER_TIMEOUT_MS,
                token, "the withdrawal of " + item.describe());
    }

    /**
     * Counts how many of an item are carried, summing stack sizes rather than inventory slots.
     *
     * @param item the item to count
     * @return the total quantity carried
     */
    private int inventoryQuantity(ItemRequirement item) {
        return ctx.inventory().stream()
                .map(InventoryEntity::raw)
                .filter(carried -> matches(carried, item))
                .mapToInt(ContainerItem::getQuantity)
                .sum();
    }

    /**
     * Counts how many of an item are in the bank.
     *
     * @param item the item to count
     * @return the quantity banked, or zero when absent
     */
    private int bankQuantity(ItemRequirement item) {
        BankEntity entity = findInBank(item);
        return entity == null ? 0 : entity.getQuantity();
    }

    /**
     * Reports whether a carried item satisfies a requirement, including its noted form.
     *
     * @param carried the item in the container
     * @param item the requirement to match against
     * @return true when the item satisfies the requirement
     */
    private boolean matches(ContainerItem carried, ItemRequirement item) {
        if (carried == null) {
            return false;
        }

        boolean identityMatches = item.byId()
                ? carried.getId() == item.getId()
                : carried.getName() != null && carried.getName().equalsIgnoreCase(item.getName());

        if (!identityMatches) {
            return false;
        }

        switch (item.getNoted()) {
            case NOTED:
                return carried.isNoted();
            case UNNOTED:
                return !carried.isNoted();
            default:
                return true;
        }
    }

    /**
     * Finds an item in the bank.
     *
     * @param item the requirement to resolve
     * @return the bank entity, or null when absent
     */
    private BankEntity findInBank(ItemRequirement item) {
        return (item.byId()
                ? ctx.bank().withId(item.getId()).first()
                : ctx.bank().withName(item.getName()).first())
                .orElse(null);
    }

    /**
     * Finds an item in the inventory.
     *
     * @param item the requirement to resolve
     * @return the inventory entity, or null when absent
     */
    private InventoryEntity findInInventory(ItemRequirement item) {
        return (item.byId()
                ? ctx.inventory().withId(item.getId()).first()
                : ctx.inventory().withName(item.getName()).first())
                .orElse(null);
    }

    /**
     * Finds a carried item through the bank-side inventory view.
     *
     * <p>This is a different container from {@link #findInInventory}, not a synonym: while the bank is
     * open the inventory is drawn under a different parent widget, and depositing has to go through
     * this view to resolve the right widget actions.</p>
     *
     * @param item the requirement to resolve
     * @return the bank-side inventory entity, or null when the item is not carried
     */
    private BankInventoryEntity findInBankInventory(ItemRequirement item) {
        return (item.byId()
                ? ctx.bankInventory().withId(item.getId()).first()
                : ctx.bankInventory().withName(item.getName()).first())
                .orElse(null);
    }

    /**
     * Finds a carried item through the equipment query, which is what exposes the wield and wear
     * actions.
     *
     * @param item the requirement to resolve
     * @return the equippable entity, or null when the item is not carried
     */
    private EquipmentEntity findEquippableInInventory(ItemRequirement item) {
        return (item.byId()
                ? ctx.equipment().inInventory().withId(item.getId()).first()
                : ctx.equipment().inInventory().withName(item.getName()).first())
                .orElse(null);
    }

    /**
     * Reports whether an item is currently worn.
     *
     * @param item the requirement to check
     * @return true when the item is equipped
     */
    private boolean isWearing(ItemRequirement item) {
        return item.byId()
                ? ctx.equipment().isWearing(item.getId())
                : ctx.equipment().isWearing(item.getName());
    }

    /**
     * Finds any carried item that can be dropped.
     *
     * @return a droppable item, or null when nothing carried can be dropped
     */
    private InventoryEntity findDroppableItem() {
        return ctx.inventory().withAction("Drop").first().orElse(null);
    }

    /**
     * Reports whether an NPC satisfying the requirement is in the scene.
     *
     * @param requirement the NPC requirement to check
     * @return true when a matching NPC is nearby
     */
    private boolean isNpcPresent(NpcRequirement requirement) {
        for (String name : requirement.getAnyOfNames()) {
            NpcQuery query = ctx.npcs().within(requirement.getWithinTiles());
            query = requirement.isNameContains() ? query.nameContains(name) : query.withName(name);

            if (requirement.getWithAction() != null) {
                query = query.withAction(requirement.getWithAction());
            }

            if (query.isPresent()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Reports whether the client is logged into a world.
     *
     * @return true when the game state is logged in
     */
    private boolean isLoggedIn() {
        GameState state = ctx.runOnClientThread(() -> ctx.getClient().getGameState());
        return state == GameState.LOGGED_IN;
    }

    /**
     * Emits a progress description when a sink was supplied.
     *
     * @param onPhase the sink, may be null
     * @param phase the description
     */
    private void report(Consumer<String> onPhase, String phase) {
        log.info("Precondition: {}", phase);
        if (onPhase != null) {
            onPhase.accept(phase);
        }
    }
}
