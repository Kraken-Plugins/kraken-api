package com.kraken.api.simulation;

import com.kraken.api.service.magic.CastableSpell;
import lombok.Getter;
import net.runelite.api.Prayer;
import net.runelite.api.coords.WorldPoint;

import java.util.List;
import java.util.Objects;

/**
 * Player action used by simulation ticks.
 */
@Getter
public final class SimulationAction {
    /**
     * Action category used by the simulation engine and execution adapter.
     */
    public enum Type {
        MOVE,
        SWITCH_PRAYER,
        EQUIP_ITEM,
        INVENTORY_INTERACT,
        CAST_SPELL,
        CUSTOM
    }

    public static final SimulationAction WAIT = new SimulationAction(Type.MOVE, 0, 0, false, null, null, null, 0, false, null, null, null);
    public static final SimulationAction NORTH = new SimulationAction(Type.MOVE, 0, 1, false, null, null, null, 0, false, null, null, null);
    public static final SimulationAction SOUTH = new SimulationAction(Type.MOVE, 0, -1, false, null, null, null, 0, false, null, null, null);
    public static final SimulationAction EAST = new SimulationAction(Type.MOVE, 1, 0, false, null, null, null, 0, false, null, null, null);
    public static final SimulationAction WEST = new SimulationAction(Type.MOVE, -1, 0, false, null, null, null, 0, false, null, null, null);
    public static final SimulationAction NORTH_EAST = new SimulationAction(Type.MOVE, 1, 1, false, null, null, null, 0, false, null, null, null);
    public static final SimulationAction NORTH_WEST = new SimulationAction(Type.MOVE, -1, 1, false, null, null, null, 0, false, null, null, null);
    public static final SimulationAction SOUTH_EAST = new SimulationAction(Type.MOVE, 1, -1, false, null, null, null, 0, false, null, null, null);
    public static final SimulationAction SOUTH_WEST = new SimulationAction(Type.MOVE, -1, -1, false, null, null, null, 0, false, null, null, null);

    private static final List<SimulationAction> STANDARD_WALK_ACTIONS =
            List.of(WAIT, NORTH, SOUTH, EAST, WEST, NORTH_EAST, NORTH_WEST, SOUTH_EAST, SOUTH_WEST);

    private final Type type;
    private final int dx;
    private final int dy;
    private final boolean run;
    private final Prayer prayer;
    private final Integer itemId;
    private final String inventoryAction;
    private final int healAmount;
    private final boolean consumeInventoryItem;
    private final CastableSpell spell;
    private final Integer targetNpcIndex;
    private final String customActionId;

    private SimulationAction(
            Type type,
            int dx,
            int dy,
            boolean run,
            Prayer prayer,
            Integer itemId,
            String inventoryAction,
            int healAmount,
            boolean consumeInventoryItem,
            CastableSpell spell,
            Integer targetNpcIndex,
            String customActionId
    ) {
        if (type == null) {
            throw new IllegalArgumentException("type cannot be null");
        }
        if ((dx < -1 || dx > 1 || dy < -1 || dy > 1) && type == Type.MOVE) {
            throw new IllegalArgumentException("dx and dy must be in range [-1, 1] for movement actions");
        }
        if (healAmount < 0) {
            throw new IllegalArgumentException("healAmount must be >= 0");
        }
        this.type = type;
        this.dx = dx;
        this.dy = dy;
        this.run = run;
        this.prayer = prayer;
        this.itemId = itemId;
        this.inventoryAction = normalize(inventoryAction);
        this.healAmount = healAmount;
        this.consumeInventoryItem = consumeInventoryItem;
        this.spell = spell;
        this.targetNpcIndex = targetNpcIndex;
        this.customActionId = normalize(customActionId);
    }

    /**
     * Creates (or reuses) a walk action with one-tile directional deltas.
     *
     * @param dx x step in range [-1, 1].
     * @param dy y step in range [-1, 1].
     * @return canonical walk action when available, otherwise a new custom walk action.
     */
    public static SimulationAction move(int dx, int dy) {
        if (dx == 0 && dy == 0) {
            return WAIT;
        }
        if (dx == NORTH.dx && dy == NORTH.dy) {
            return NORTH;
        }
        if (dx == SOUTH.dx && dy == SOUTH.dy) {
            return SOUTH;
        }
        if (dx == EAST.dx && dy == EAST.dy) {
            return EAST;
        }
        if (dx == WEST.dx && dy == WEST.dy) {
            return WEST;
        }
        if (dx == NORTH_EAST.dx && dy == NORTH_EAST.dy) {
            return NORTH_EAST;
        }
        if (dx == NORTH_WEST.dx && dy == NORTH_WEST.dy) {
            return NORTH_WEST;
        }
        if (dx == SOUTH_EAST.dx && dy == SOUTH_EAST.dy) {
            return SOUTH_EAST;
        }
        if (dx == SOUTH_WEST.dx && dy == SOUTH_WEST.dy) {
            return SOUTH_WEST;
        }
        return new SimulationAction(Type.MOVE, dx, dy, false, null, null, null, 0, false, null, null, null);
    }

    /**
     * Creates a run movement action with one-tile directional deltas.
     *
     * @param dx x step in range [-1, 1].
     * @param dy y step in range [-1, 1].
     * @return run movement action.
     */
    public static SimulationAction run(int dx, int dy) {
        if (dx == 0 && dy == 0) {
            return WAIT;
        }
        return new SimulationAction(Type.MOVE, dx, dy, true, null, null, null, 0, false, null, null, null);
    }

    /**
     * Creates a prayer-switch action.
     *
     * @param prayer overhead prayer to activate.
     * @return prayer action.
     */
    public static SimulationAction switchPrayer(Prayer prayer) {
        if (prayer == null) {
            throw new IllegalArgumentException("prayer cannot be null");
        }
        return new SimulationAction(Type.SWITCH_PRAYER, 0, 0, false, prayer, null, null, 0, false, null, null, null);
    }

    /**
     * Creates an equip-item action.
     *
     * @param itemId inventory item id to equip.
     * @return equip action.
     */
    public static SimulationAction equipItem(int itemId) {
        if (itemId < 0) {
            throw new IllegalArgumentException("itemId must be >= 0");
        }
        return new SimulationAction(Type.EQUIP_ITEM, 0, 0, false, null, itemId, null, 0, false, null, null, null);
    }

    /**
     * Creates a generic inventory interaction action.
     *
     * @param itemId inventory item id to interact with.
     * @param action inventory action text (for example, Eat/Drink/Use).
     * @return inventory interaction action.
     */
    public static SimulationAction inventoryInteract(int itemId, String action) {
        if (itemId < 0) {
            throw new IllegalArgumentException("itemId must be >= 0");
        }
        return new SimulationAction(Type.INVENTORY_INTERACT, 0, 0, false, null, itemId, action, 0, false, null, null, null);
    }

    /**
     * Creates a food-eat action.
     *
     * @param itemId inventory food item id.
     * @param healAmount simulated heal amount applied when this action is taken.
     * @return eat action.
     */
    public static SimulationAction eat(int itemId, int healAmount) {
        if (itemId < 0) {
            throw new IllegalArgumentException("itemId must be >= 0");
        }
        return new SimulationAction(Type.INVENTORY_INTERACT, 0, 0, false, null, itemId, "Eat", healAmount, true, null, null, null);
    }

    /**
     * Creates a spell cast action with no explicit target.
     *
     * @param spell spell to cast.
     * @return cast action.
     */
    public static SimulationAction castSpell(CastableSpell spell) {
        if (spell == null) {
            throw new IllegalArgumentException("spell cannot be null");
        }
        return new SimulationAction(Type.CAST_SPELL, 0, 0, false, null, null, null, 0, false, spell, null, null);
    }

    /**
     * Creates a spell cast action targeting an npc index.
     *
     * @param spell spell to cast.
     * @param targetNpcIndex target npc index.
     * @return cast action.
     */
    public static SimulationAction castSpellOnNpc(CastableSpell spell, int targetNpcIndex) {
        if (spell == null) {
            throw new IllegalArgumentException("spell cannot be null");
        }
        if (targetNpcIndex < 0) {
            throw new IllegalArgumentException("targetNpcIndex must be >= 0");
        }
        return new SimulationAction(Type.CAST_SPELL, 0, 0, false, null, null, null, 0, false, spell, targetNpcIndex, null);
    }

    /**
     * Creates a custom no-op simulation action marker for external adapters/evaluators.
     *
     * @param customActionId action identifier.
     * @return custom action.
     */
    public static SimulationAction custom(String customActionId) {
        String normalized = normalize(customActionId);
        if (normalized == null) {
            throw new IllegalArgumentException("customActionId cannot be blank");
        }
        return new SimulationAction(Type.CUSTOM, 0, 0, false, null, null, null, 0, false, null, null, normalized);
    }

    /**
     * @return canonical walk-only action set including {@link #WAIT}.
     */
    public static List<SimulationAction> standardWalkActions() {
        return STANDARD_WALK_ACTIONS;
    }

    /**
     * @return true when this action is a movement action.
     */
    public boolean isMovement() {
        return type == Type.MOVE;
    }

    /**
     * @return true when this is the canonical wait action.
     */
    public boolean isWait() {
        return this == WAIT || (type == Type.MOVE && dx == 0 && dy == 0 && !run);
    }

    /**
     * Converts movement action into world-space destination from an origin.
     *
     * @param origin source world point.
     * @return destination world point after applying movement.
     */
    public WorldPoint destinationFrom(WorldPoint origin) {
        if (origin == null) {
            throw new IllegalArgumentException("origin cannot be null");
        }
        if (!isMovement()) {
            return origin;
        }
        int steps = run ? 2 : 1;
        return new WorldPoint(origin.getX() + (dx * steps), origin.getY() + (dy * steps), origin.getPlane());
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SimulationAction)) {
            return false;
        }
        SimulationAction that = (SimulationAction) other;
        return dx == that.dx
                && dy == that.dy
                && run == that.run
                && healAmount == that.healAmount
                && consumeInventoryItem == that.consumeInventoryItem
                && type == that.type
                && prayer == that.prayer
                && Objects.equals(itemId, that.itemId)
                && Objects.equals(inventoryAction, that.inventoryAction)
                && Objects.equals(spell, that.spell)
                && Objects.equals(targetNpcIndex, that.targetNpcIndex)
                && Objects.equals(customActionId, that.customActionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                type,
                dx,
                dy,
                run,
                prayer,
                itemId,
                inventoryAction,
                healAmount,
                consumeInventoryItem,
                spell,
                targetNpcIndex,
                customActionId
        );
    }

    @Override
    public String toString() {
        switch (type) {
            case MOVE:
                return "SimulationAction{type=MOVE,dx=" + dx + ",dy=" + dy + ",run=" + run + "}";
            case SWITCH_PRAYER:
                return "SimulationAction{type=SWITCH_PRAYER,prayer=" + prayer + "}";
            case EQUIP_ITEM:
                return "SimulationAction{type=EQUIP_ITEM,itemId=" + itemId + "}";
            case INVENTORY_INTERACT:
                return "SimulationAction{type=INVENTORY_INTERACT,itemId=" + itemId + ",action=" + inventoryAction + ",heal=" + healAmount + "}";
            case CAST_SPELL:
                return "SimulationAction{type=CAST_SPELL,spell=" + (spell == null ? null : spell.getName()) + ",targetNpcIndex=" + targetNpcIndex + "}";
            default:
                return "SimulationAction{type=CUSTOM,id=" + customActionId + "}";
        }
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
