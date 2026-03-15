package com.kraken.api.query.container.bank;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractEntity;
import com.kraken.api.query.container.ContainerItem;
import com.kraken.api.service.bank.DepositBoxService;
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
     * Deposits the filtered equipped item from the player's inventory into the bank deposit box. This will only
     * work when {@code ctx.depositBox().inEquipment();} is used to filter the deposit box.
     *
     * <p>This method interacts with the game's widget system to send a deposit request for a single piece
     * of equipment.
     *
     * @return {@code true} if the deposit operation was executed successfully, {@code false} otherwise.
     */
    public boolean depositEquipment() {
        // raw().getSlot() will be the widget id for the slot that we need to interact with
        return ctx.widgets().get(raw().getSlot()).interact(2, raw().getSlot(), -1, -1);
    }

    /**
     * Deposits one of the given item from the players inventory into the bank deposit box. This will **NOT** work
     * for depositing equipment if {@code ctx.depositBox().inEquipment();} is used to filter the deposit box.
     * @return true if the deposit was successful and false otherwise.
     */
    public boolean depositOne() {
        return deposit(1);
    }

    /**
     * Deposits five of the given items from the players' inventory into the bank deposit box. This will **NOT** work
     * for depositing equipment if {@code ctx.depositBox().inEquipment();} is used to filter the deposit box.
     * @return true if the deposit was successful and false otherwise.
     */
    public boolean depositFive() {
        return deposit(5);
    }

    /**
     * Deposits ten of the given items from the players' inventory into the bank deposit box. This will **NOT** work
     * for depositing equipment if {@code ctx.depositBox().inEquipment();} is used to filter the deposit box.
     * @return true if the deposit was successful and false otherwise.
     */
    public boolean depositTen() {
        return deposit(10);
    }

    /**
     * Deposits X amount of an item from the players inventory into the bank deposit box. This will **NOT** work
     * or depositing equipment if {@code ctx.depositBox().inEquipment();} is used to filter the deposit box.
     * @param amount The amount of the item to deposit.
     * @return true if the deposit was successful and false otherwise.
     */
    public boolean depositX(int amount) {
        ctx.getService(DepositBoxService.class).setQuantity(amount);
        return ctx.widgets()
                .fromClient(InterfaceID.BankDepositbox.INVENTORY)
                .interact(1, InterfaceID.BankDepositbox.INVENTORY, raw().getSlot(), getId());
    }

    /**
     * Deposits all of the given item from the players inventory into the bank deposit box. This will **NOT** work
     * for depositing equipment if {@code ctx.depositBox().inEquipment();} is used to filter the deposit box.
     * @return true if the deposit was successful and false otherwise.
     */
    public boolean depositAll() {
        return deposit(-1);
    }

    /**
     * Deposits a set amount of the given item from the players' inventory to the bank. If the amount is
     * not one of: 1, 5, or 10, then all the given items will be deposited by default.
     * @param amount The amount of the item to deposit: 1, 5, 10, or any other integer for all the item
     * @return True if the deposit was successful and false otherwise
     */
    public boolean deposit(int amount) {
        if(ctx.getService(DepositBoxService.class).isClosed()) return false;
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
     * Checks if the specified action is available in the inventory actions of the container item.
     *
     * <p>This method retrieves the item's inventory actions and verifies if any action matches
     * the provided action string, ignoring case sensitivity.</p>
     *
     * @param action a {@code String} representing the name of the action to check for.
     *               It is case-insensitive, and null or empty actions are ignored.
     * @return {@code true} if the specified action is available in the item's inventory actions;
     *         {@code false} otherwise.
     */
    public boolean hasAction(String action) {
        ContainerItem item = raw();
        return Arrays.stream(item.getInventoryActions()).anyMatch(a -> a != null && a.equalsIgnoreCase(action));
    }
}