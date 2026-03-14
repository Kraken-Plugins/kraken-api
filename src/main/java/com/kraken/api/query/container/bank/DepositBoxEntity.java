package com.kraken.api.query.container.bank;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractEntity;
import com.kraken.api.query.container.ContainerItem;
import com.kraken.api.service.ui.dialogue.DialogueService;
import net.runelite.api.gameval.InterfaceID;

import java.util.Arrays;

public class DepositBoxEntity extends AbstractEntity<ContainerItem> {

    public DepositBoxEntity(Context ctx, ContainerItem raw) {
        super(ctx, raw);
    }

    @Override
    public String getName() {
        ContainerItem item = raw();
        return item != null ? item.getName() : null;
    }

    @Override
    public boolean interact(String action) {
        ContainerItem raw = raw();
        if (raw == null) return false;
        ctx.getInteractionManager().interact(raw, action);
        return true;
    }

    @Override
    public int getId() {
        ContainerItem item = raw();
        return item != null ? item.getId() : -1;
    }

    /**
     * Returns the quantity of the item in your inventory when the bank deposit box interface is open.
     * @return Int the quantity of the bank inventory entity (this will be the stack size of the item if it is noted) or
     * the value of the item if it's coins.
     */
    public int count() {
        ContainerItem raw = raw();
        return raw != null ? raw.getQuantity() : -1;
    }

    /**
     * Deposits one of the given item from the players inventory into the bank deposit box.
     * @return true if the deposit was successful and false otherwise.
     */
    public boolean depositOne() {
        return deposit(1);
    }

    /**
     * Deposits five of the given items from the players inventory into the bank deposit box.
     * @return true if the deposit was successful and false otherwise.
     */
    public boolean depositFive() {
        return deposit(5);
    }

    /**
     * Deposits ten of the given items from the players inventory into the bank deposit box.
     * @return true if the deposit was successful and false otherwise.
     */
    public boolean depositTen() {
        return deposit(10);
    }

    /**
     * Deposits X amount of an item from the players inventory into the bank deposit box.
     * @param amount The amount of the item to deposit.
     * @return true if the deposit was successful and false otherwise.
     */
    public boolean depositX(int amount) {
        setQuantity(amount);
        return ctx.widgets()
                .fromClient(InterfaceID.BankDepositbox.INVENTORY)
                .interact(1, InterfaceID.BankDepositbox.INVENTORY, raw().getSlot(), getId());
    }

    /**
     * Deposits all of the given item from the players inventory into the bank deposit box.
     * @return true if the deposit was successful and false otherwise.
     */
    public boolean depositAll() {
        return deposit(-1);
    }

    /**
     * Deposits a set amount of the given item from the players inventory to the bank. If the amount is
     * not one of: 1, 5, or 10 then all of the given item will be deposited by default.
     * @param amount The amount of the item to deposit: 1, 5, 10, or any other integer for all of the item
     * @return True if the deposit was successful and false otherwise
     */
    public boolean deposit(int amount) {
        if(!ctx.depositBox().isOpen()) return false;
        ContainerItem raw = raw();

        switch(amount) {
            case 1:
                ctx.getInteractionManager().interact(raw, "Deposit-1");
                return true;
            case 5:
                ctx.getInteractionManager().interact(raw, "Deposit-5");
                return true;
            case 10:
                ctx.getInteractionManager().interact(raw, "Deposit-10");
                return true;
            default:
                ctx.getInteractionManager().interact(raw, "Deposit-All");
                return true;
        }
    }

    /**
     * Returns true if the inventory item has the specified action. i.e "Swordfish" will have the action "Eat", "Drop", and "Examine" but not "Drink"
     *
     * @param action The action to check for
     * @return True if the item has the action and false otherwise
     */
    public boolean hasAction(String action) {
        ContainerItem item = raw();
        return Arrays.stream(item.getInventoryActions()).anyMatch(a -> a != null && a.equalsIgnoreCase(action));
    }

    private boolean setQuantity(int amount) {
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
}