package plugins.colosseum.simulation.live;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.query.container.inventory.InventoryEntity;
import com.kraken.api.query.equipment.EquipmentEntity;
import com.kraken.api.query.npc.NpcEntity;
import com.kraken.api.service.movement.MovementService;
import com.kraken.api.service.prayer.PrayerService;
import plugins.colosseum.simulation.LoadoutConfig;
import plugins.colosseum.simulation.plan.Decision;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Prayer;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

import java.util.Objects;

/**
 * Executes a {@link Decision} through the Kraken API in priority order: prayer first
 * (the most tick-critical action), then consumables (survival), then gear swaps, then the
 * attack or movement click. Attack and movement are mutually exclusive per tick, mirroring
 * both the engine model and the real client (a new click replaces the interaction).
 */
@Slf4j
@Singleton
public final class Executor {
    private final Context ctx;
    private final PrayerService prayerService;
    private final MovementService movementService;

    /**
     * Creates the executor.
     *
     * @param ctx kraken context.
     * @param prayerService prayer service.
     * @param movementService movement service.
     */
    @Inject
    public Executor(Context ctx, PrayerService prayerService, MovementService movementService) {
        this.ctx = Objects.requireNonNull(ctx, "ctx");
        this.prayerService = Objects.requireNonNull(prayerService, "prayerService");
        this.movementService = Objects.requireNonNull(movementService, "movementService");
    }

    /**
     * Executes every component of the decision for this tick.
     *
     * @param decision planner decision.
     * @param loadout loadout used to resolve item ids.
     * @return true when at least one action was issued.
     */
    public boolean execute(Decision decision, LoadoutConfig loadout) {
        if (decision == null || loadout == null) {
            return false;
        }
        Boolean executed = ctx.runOnClientThread(() -> {
            boolean any = false;
            any |= executePrayer(decision);
            any |= executeConsumables(decision, loadout);
            any |= executeGear(decision, loadout);
            if (decision.isUseSpec()) {
                ctx.getLocalPlayer().toggleSpecialAttack(50);
                any = true;
            }
            if (decision.hasAttack()) {
                any |= executeAttack(decision);
            } else {
                any |= executeMove(decision);
            }
            return any;
        });
        return Boolean.TRUE.equals(executed);
    }

    private boolean executePrayer(Decision decision) {
        Prayer prayer = decision.getPrayerToActivate();
        if (prayer != null) {
            return prayerService.toggle(prayer, true);
        }
        if (decision.isPrayerOff()) {
            return prayerService.deactivateAll(true, true, 3);
        }
        return false;
    }

    private boolean executeConsumables(Decision decision, LoadoutConfig loadout) {
        boolean any = false;
        if (decision.isEatFood()) {
            any |= interactFirst(loadout.getFoodItemIds(), "Eat");
        }
        if (decision.isEatCombo()) {
            any |= interactFirst(loadout.getComboItemIds(), "Eat");
        }
        if (decision.isSipBrew()) {
            any |= interactFirst(loadout.getBrewItemIds(), "Drink");
        }
        if (decision.isSipRestore()) {
            any |= interactFirst(loadout.getRestoreItemIds(), "Drink");
        }
        return any;
    }

    private boolean executeGear(Decision decision, LoadoutConfig loadout) {
        int setIndex = decision.getGearSetIndex();
        if (setIndex < 0 || setIndex >= loadout.gearSetCount()) {
            return false;
        }
        boolean any = false;
        for (int itemId : loadout.gearSet(setIndex).getEquipItemIds()) {
            EquipmentEntity item = ctx.equipment().inInventory().withId(itemId).first();
            if (item != null) {
                any |= item.wieldOrWear();
            }
        }
        return any;
    }

    private boolean executeAttack(Decision decision) {
        int npcIndex = decision.getAttackNpcRuneliteIndex();
        // Freshest-possible redundancy guard: if the client says we are already fighting
        // this exact NPC, re-clicking it accomplishes nothing.
        Player localPlayer = ctx.getClient().getLocalPlayer();
        if (localPlayer != null) {
            Actor interacting = localPlayer.getInteracting();
            if (interacting instanceof NPC && ((NPC) interacting).getIndex() == npcIndex) {
                return false;
            }
        }
        NpcEntity npc = ctx.npcs()
                .filter(n -> n != null && n.raw() != null && n.raw().getIndex() == npcIndex)
                .first();
        if (npc == null) {
            log.debug("Attack target npc index {} not found", npcIndex);
            return false;
        }
        return npc.interact("Attack");
    }

    private boolean executeMove(Decision decision) {
        WorldPoint destination = decision.getMoveDestination();
        if (destination == null) {
            return false;
        }
        // Skip redundant clicks when the client is already pathing to this exact tile.
        LocalPoint currentDestination = ctx.getClient().getLocalDestinationLocation();
        if (currentDestination != null) {
            WorldPoint current = WorldPoint.fromLocal(ctx.getClient(), currentDestination);
            Player localPlayer = ctx.getClient().getLocalPlayer();
            if (destination.equals(current)
                    && (localPlayer == null || !destination.equals(localPlayer.getWorldLocation()))) {
                return false;
            }
        }
        movementService.moveTo(destination);
        return true;
    }

    private boolean interactFirst(int[] itemIds, String action) {
        if (itemIds == null) {
            return false;
        }
        for (int itemId : itemIds) {
            InventoryEntity item = ctx.inventory().withId(itemId).first();
            if (item != null) {
                return item.interact(action);
            }
        }
        return false;
    }
}
