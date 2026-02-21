package com.kraken.api.simulation;

import com.kraken.api.service.map.WorldPointService;
import com.kraken.api.service.magic.CastableSpell;
import lombok.Getter;
import net.runelite.api.Prayer;
import net.runelite.api.coords.WorldPoint;

import java.util.Objects;

/**
 * Action edge used while expanding and executing simulation trees.
 */
@Getter
public final class SimulationAction {
    /**
     * Simulation action kind.
     */
    public enum Type {
        WAIT,
        MOVE,
        SWITCH_PRAYER,
        EQUIP_ITEM,
        NPC_INTERACT,
        CAST_SPELL,
        CUSTOM
    }

    /**
     * Canonical wait action.
     */
    public static final SimulationAction WAIT = new SimulationAction(
            Type.WAIT,
            -1,
            false,
            null,
            null,
            null,
            0,
            false,
            null,
            null,
            null,
            null
    );

    private final Type type;
    private final int targetPackedPoint;
    private final boolean run;
    private final Prayer prayer;
    private final Integer itemId;
    private final String inventoryAction;
    private final int healAmount;
    private final boolean consumeInventoryItem;
    private final CastableSpell spell;
    private final Integer targetNpcIndex;
    private final String npcInteractionAction;
    private final String customActionId;

    private SimulationAction(
            Type type,
            int targetPackedPoint,
            boolean run,
            Prayer prayer,
            Integer itemId,
            String inventoryAction,
            int healAmount,
            boolean consumeInventoryItem,
            CastableSpell spell,
            Integer targetNpcIndex,
            String npcInteractionAction,
            String customActionId
    ) {
        if (type == null) {
            throw new IllegalArgumentException("type cannot be null");
        }
        if (healAmount < 0) {
            throw new IllegalArgumentException("healAmount must be >= 0");
        }
        this.type = type;
        this.targetPackedPoint = targetPackedPoint;
        this.run = run;
        this.prayer = prayer;
        this.itemId = itemId;
        this.inventoryAction = normalize(inventoryAction);
        this.healAmount = healAmount;
        this.consumeInventoryItem = consumeInventoryItem;
        this.spell = spell;
        this.targetNpcIndex = targetNpcIndex;
        this.npcInteractionAction = normalize(npcInteractionAction);
        this.customActionId = normalize(customActionId);
    }

    /**
     * Creates a move action to a destination tile.
     *
     * @param destination destination world point.
     * @return move action.
     */
    public static SimulationAction moveTo(WorldPoint destination) {
        if (destination == null) {
            throw new IllegalArgumentException("destination cannot be null");
        }
        return moveToPacked(WorldPointService.pack(destination), false);
    }

    /**
     * Creates a run action to a destination tile.
     *
     * @param destination destination world point.
     * @return run action.
     */
    public static SimulationAction runTo(WorldPoint destination) {
        if (destination == null) {
            throw new IllegalArgumentException("destination cannot be null");
        }
        return moveToPacked(WorldPointService.pack(destination), true);
    }

    /**
     * Creates a move action using a packed destination.
     *
     * @param packedWorldPoint packed destination.
     * @param run true for 2-step movement per tick.
     * @return move action.
     */
    public static SimulationAction moveToPacked(int packedWorldPoint, boolean run) {
        return new SimulationAction(Type.MOVE, packedWorldPoint, run, null, null, null, 0, false, null, null, null, null);
    }

    /**
     * Creates a prayer switch action.
     *
     * @param prayer overhead prayer to enable.
     * @return prayer switch action.
     */
    public static SimulationAction switchPrayer(Prayer prayer) {
        if (prayer == null) {
            throw new IllegalArgumentException("prayer cannot be null");
        }
        return new SimulationAction(Type.SWITCH_PRAYER, -1, false, prayer, null, null, 0, false, null, null, null, null);
    }

    /**
     * Creates an equip action.
     *
     * @param itemId inventory item id.
     * @return equip action.
     */
    public static SimulationAction equipItem(int itemId) {
        if (itemId < 0) {
            throw new IllegalArgumentException("itemId must be >= 0");
        }
        return new SimulationAction(Type.EQUIP_ITEM, -1, false, null, itemId, null, 0, false, null, null, null, null);
    }

    /**
     * Creates an npc interaction action.
     *
     * @param npcIndex target npc index.
     * @param action interaction action text.
     * @return npc interaction action.
     */
    public static SimulationAction interactNpc(int npcIndex, String action) {
        if (npcIndex < 0) {
            throw new IllegalArgumentException("npcIndex must be >= 0");
        }
        String normalizedAction = normalize(action);
        if (normalizedAction == null) {
            throw new IllegalArgumentException("action cannot be blank");
        }
        return new SimulationAction(Type.NPC_INTERACT, -1, false, null, null, null, 0, false, null, npcIndex, normalizedAction, null);
    }

    /**
     * Creates an untargeted spell cast.
     *
     * @param spell spell.
     * @return spell action.
     */
    public static SimulationAction castSpell(CastableSpell spell) {
        if (spell == null) {
            throw new IllegalArgumentException("spell cannot be null");
        }
        return new SimulationAction(Type.CAST_SPELL, -1, false, null, null, null, 0, false, spell, null, null, null);
    }

    /**
     * Creates a targeted spell cast.
     *
     * @param spell spell.
     * @param targetNpcIndex target npc index.
     * @return spell action.
     */
    public static SimulationAction castSpellOnNpc(CastableSpell spell, int targetNpcIndex) {
        if (spell == null) {
            throw new IllegalArgumentException("spell cannot be null");
        }
        if (targetNpcIndex < 0) {
            throw new IllegalArgumentException("targetNpcIndex must be >= 0");
        }
        return new SimulationAction(Type.CAST_SPELL, -1, false, null, null, null, 0, false, spell, targetNpcIndex, null, null);
    }

    /**
     * Creates a custom action marker.
     *
     * @param customActionId custom action id.
     * @return custom action.
     */
    public static SimulationAction custom(String customActionId) {
        String normalized = normalize(customActionId);
        if (normalized == null) {
            throw new IllegalArgumentException("customActionId cannot be blank");
        }
        return new SimulationAction(Type.CUSTOM, -1, false, null, null, null, 0, false, null, null, null, normalized);
    }

    /**
     * @return true when this action is movement.
     */
    public boolean isMovement() {
        return type == Type.MOVE;
    }

    /**
     * @return true when this action is wait.
     */
    public boolean isWait() {
        return type == Type.WAIT;
    }

    /**
     * Resolves movement destination.
     *
     * @return destination world point, or null for non-movement actions.
     */
    public WorldPoint getMovementDestination() {
        if (!isMovement()) {
            return null;
        }
        return WorldPointService.unpack(targetPackedPoint);
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
        return targetPackedPoint == that.targetPackedPoint
                && run == that.run
                && healAmount == that.healAmount
                && consumeInventoryItem == that.consumeInventoryItem
                && type == that.type
                && prayer == that.prayer
                && Objects.equals(itemId, that.itemId)
                && Objects.equals(inventoryAction, that.inventoryAction)
                && Objects.equals(spell, that.spell)
                && Objects.equals(targetNpcIndex, that.targetNpcIndex)
                && Objects.equals(npcInteractionAction, that.npcInteractionAction)
                && Objects.equals(customActionId, that.customActionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                type,
                targetPackedPoint,
                run,
                prayer,
                itemId,
                inventoryAction,
                healAmount,
                consumeInventoryItem,
                spell,
                targetNpcIndex,
                npcInteractionAction,
                customActionId
        );
    }

    @Override
    public String toString() {
        switch (type) {
            case WAIT:
                return "SimulationAction{type=WAIT}";
            case MOVE:
                return "SimulationAction{type=MOVE,target=" + getMovementDestination() + ",run=" + run + "}";
            case SWITCH_PRAYER:
                return "SimulationAction{type=SWITCH_PRAYER,prayer=" + prayer + "}";
            case EQUIP_ITEM:
                return "SimulationAction{type=EQUIP_ITEM,itemId=" + itemId + "}";
            case NPC_INTERACT:
                return "SimulationAction{type=NPC_INTERACT,npcIndex=" + targetNpcIndex + ",action=" + npcInteractionAction + "}";
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
