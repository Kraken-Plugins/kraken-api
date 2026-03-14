package com.kraken.api.query.container.bank;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractQuery;
import com.kraken.api.query.container.ContainerItem;
import com.kraken.api.query.equipment.EquipmentEntity;
import com.kraken.api.query.widget.WidgetEntity;
import com.kraken.api.service.ui.dialogue.DialogueService;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;


public class DepositBoxQuery extends AbstractQuery<DepositBoxEntity, DepositBoxQuery, ContainerItem> {

    public DepositBoxQuery(Context ctx) {
        super(ctx);
    }

    @Override
    protected Supplier<Stream<DepositBoxEntity>> source() {
        return () -> {
            List<DepositBoxEntity> depositBoxItems = ctx.runOnClientThread(() -> {
                WidgetEntity depositBox = ctx.widgets().fromClient(InterfaceID.BankDepositbox.INVENTORY);

                if(depositBox == null || !depositBox.isVisible()) {
                    return Collections.emptyList();
                } // CS2 229 For hovering over close button
                // CS2

                Widget[] depositBoxWidgets = depositBox.raw().getDynamicChildren();
                List<DepositBoxEntity> entities = new ArrayList<>();
                for (int i = 0; i < depositBoxWidgets.length; i++) {
                    final Widget widget = depositBoxWidgets[i];
                    if (widget.getItemId() == -1 || widget.getItemId() == 6512) continue;
                    final ItemComposition itemComposition = ctx.getClient().getItemDefinition(widget.getItemId());
                    entities.add(new DepositBoxEntity(ctx, new ContainerItem(new Item(widget.getItemId(), widget.getItemQuantity()), itemComposition, i, ctx, widget, null)));
                }
                return entities;
            });

            return depositBoxItems.stream();
        };
    }

    /**
     * Determines whether the bank interface is currently open.
     *
     * <p>This method interacts with the {@code WidgetQuery} to check the status of the deposit box interface.
     * The deposit box is considered open if the corresponding interface is visible and active in the client.
     *
     * @return {@code true} if the deposit box interface is open, {@code false} otherwise.
     */
    public boolean isOpen() {
        return ctx.widgets().fromClient(InterfaceID.BankDepositbox.FRAME).isVisible();
    }

    /**
     * Closes the deposit box interface if it is open.
     * @return True if the deposit box interface was closed and false otherwise
     */
    public boolean close() {
        ctx.runScript(29);
        return true;
    }

    /**
     * Deposits all items from the inventory into the deposit box
     */
    public boolean depositAll() {
        return ctx.widgets().fromClient(InterfaceID.BankDepositbox.DEPOSIT_INV)
                .interact(1, InterfaceID.BankDepositbox.DEPOSIT_INV, -1, -1);
    }

    /**
     * Deposits all worn items from the inventory into the deposit box
     */
    public boolean depositWornItems() {
        return ctx.widgets().fromClient(InterfaceID.BankDepositbox.DEPOSIT_WORN)
                .interact(1, InterfaceID.BankDepositbox.DEPOSIT_WORN, -1, -1);
    }

    /**
     * Deposits all items from the looting bag.
     */
    public boolean depositLootingBag() {
        return ctx.widgets().fromClient(InterfaceID.BankDepositbox.DEPOSIT_LOOTINGBAG)
                .interact(1, InterfaceID.BankDepositbox.DEPOSIT_LOOTINGBAG, -1, -1);
    }

    /**
     * Deposits a worn item from the specified equipment slot into the bank deposit box.
     *
     * @param slot The equipment slot to deposit from.
     * @return True if the deposit was successful and false otherwise
     */
    public boolean depositWorn(EquipmentInventorySlot slot) {
        return invokeDepositWorn(slot);
    }

    /**
     * Deposits a specific worn item by item its item id.
     *
     * @param id The item id to deposit into the deposit box.
     * @return True if the deposit was successful and false otherwise
     */
    public boolean depositWorn(int id) {
        EquipmentEntity equipment = ctx.equipment().inInterface().withId(id).first();
        if (equipment == null) {
            return false;
        }

        EquipmentInventorySlot slot = equipment.getSlot();
        if (slot == null) {
            return false;
        }

        return invokeDepositWorn(slot);
    }

    /**
     * Deposits a specific worn item into the deposit box given the items name.
     *
     * @param name The item name to deposit.
     * @return True if the deposit was successful and false otherwise.
     */
    public boolean depositWorn(String name) {
        EquipmentEntity equipment = ctx.equipment().inInterface().withName(name).first();
        if (equipment == null) {
            return false;
        }

        EquipmentInventorySlot slot = equipment.getSlot();
        if (slot == null) {
            return false;
        }

        return invokeDepositWorn(slot);
    }

    /**
     * Invokes the "deposit" action on a worn item in the equipment slot specified
     * by the given {@code EquipmentInventorySlot}. This method is used internally to provide
     * overloads to the depositWorn() method.
     *
     * @param slot The equipment slot.
     * @return True if the invocation was successful and false otherwise.
     */
    private boolean invokeDepositWorn(EquipmentInventorySlot slot) {
        int slotWidgetId = getDepositBoxWidget(slot);
        return ctx.widgets().get(slotWidgetId).interact(2, slotWidgetId, -1, -1);
    }

    /**
     * Maps an {@code EquipmentInventorySlot} to its corresponding Deposit box widget id. Given a slot
     * in the players equipment this will return the associated interface id for the deposit box equipment slot.
     *
     * @param slot The equipment slot.
     * @return The widget identifier for that slot in the deposit box interface.
     * @throws IllegalArgumentException if the slot is not a valid mappable slot.
     */
    private int getDepositBoxWidget(EquipmentInventorySlot slot) {
        switch (slot) {
            case HEAD: return InterfaceID.BankDepositbox.SLOT0;
            case CAPE: return InterfaceID.BankDepositbox.SLOT1;
            case AMULET: return InterfaceID.BankDepositbox.SLOT2;
            case WEAPON: return InterfaceID.BankDepositbox.SLOT3;
            case BODY: return InterfaceID.BankDepositbox.SLOT4;
            case SHIELD: return InterfaceID.BankDepositbox.SLOT5;
            case LEGS: return InterfaceID.BankDepositbox.SLOT7;
            case GLOVES: return InterfaceID.BankDepositbox.SLOT9;
            case BOOTS: return InterfaceID.BankDepositbox.SLOT10;
            case RING: return InterfaceID.BankDepositbox.SLOT12;
            case AMMO: return InterfaceID.BankDepositbox.SLOT13;
            default: throw new IllegalArgumentException("Unknown equipment slot provided: " + slot);
        }
    }

    /**
     * TODO This may belong in a service class since it is universal to all deposit boxes (same with deposit all, deposit worn (all methods) and deposit looting bag)
     * Sets the deposit quantity for the deposit box.
     *
     * @param amount The amount to set (-1 for all items, 1, 5, 10, or X).
     */
    public boolean setQuantity(int amount) {
        if (amount == 1) {
            return ctx.widgets().fromClient(InterfaceID.BankDepositbox._1).interact(1, InterfaceID.BankDepositbox._1, -1, -1);
        } else if (amount == 5) {
            return ctx.widgets().fromClient(InterfaceID.BankDepositbox._5).interact(1, InterfaceID.BankDepositbox._5, -1, -1);
        } else if (amount == 10) {
            return ctx.widgets().fromClient(InterfaceID.BankDepositbox._10).interact(1, InterfaceID.BankDepositbox._10, -1, -1);
        } else if (amount == -1) {
            return ctx.widgets().fromClient(InterfaceID.BankDepositbox.ALL).interact(1, InterfaceID.BankDepositbox.ALL, -1, -1);
        }

        boolean success = ctx.widgets().fromClient(InterfaceID.BankDepositbox.X).interact(1, InterfaceID.BankDepositbox.X, -1, -1);
        ctx.getService(DialogueService.class).continueNumericDialogue(amount);
        return success;
    }

    /**
     * Filters for items in the bank which have a specified item id.
     * @param id The item id to filter for
     * @return BankQuery
     */
    public DepositBoxQuery withId(int id) {
        return filter(item -> item.getId() == id);
    }

    /**
     * Filters for items that are noted (cert).
     * @return DepositBoxQuery
     */
    public DepositBoxQuery noted() {
        return filter(item -> item.raw().isNoted());
    }

    /**
     * Filters for un-noted items.
     * @return DepositBoxQuery
     */
    public DepositBoxQuery unnoted() {
        return filter(item -> !item.raw().isNoted());
    }

    /**
     * Filters for items that stack (runes, arrows, noted items).
     * @return DepositBoxQuery
     */
    public DepositBoxQuery stackable() {
        return filter(item -> item.raw().isStackable());
    }

    /**
     * Filters by item quantity. This filter is strictly greater than i.e {@code ctx.inventory().nameContains("karambwanji").quantityGreaterThan(500);}
     * will only return a {@code ContainerItem} when 501 Karambwanji's are present.
     * @param amount The amount of the stack to filter for.
     * @return DepositBoxQuery
     */
    public DepositBoxQuery quantityGreaterThan(int amount) {
        return filter(item -> item.raw().getQuantity() > amount);
    }
}