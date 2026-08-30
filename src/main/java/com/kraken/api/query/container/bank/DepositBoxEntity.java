package com.kraken.api.query.container.bank;

import com.kraken.api.Context;
import com.kraken.api.query.container.AbstractContainerEntity;
import com.kraken.api.query.container.ContainerItem;
import com.kraken.api.service.bank.DepositBoxService;
import net.runelite.api.gameval.InterfaceID;

public class DepositBoxEntity extends AbstractContainerEntity {

    public DepositBoxEntity(Context ctx, ContainerItem raw) {
        super(ctx, raw);
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
                .interact(InterfaceID.BankDepositbox.INVENTORY, raw().getSlot(), getId(), 1);
    }

    /**
     * Deposits all the given items from the players inventory into the bank deposit box. This will **NOT** work
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
            case -1:
                return ctx.getInteractionManager().interact(raw, "Deposit-All");
            case 1:
                return ctx.getInteractionManager().interact(raw, "Deposit-1");
            case 5:
                return ctx.getInteractionManager().interact(raw, "Deposit-5");
            case 10:
                return ctx.getInteractionManager().interact(raw, "Deposit-10");
            default:
                ctx.getService(DepositBoxService.class).setQuantity(amount);
                return ctx.widgets()
                        .fromClient(InterfaceID.BankDepositbox.INVENTORY)
                        .interact(InterfaceID.BankDepositbox.INVENTORY, raw().getSlot(), getId(), 1);
        }
    }

    /**
     * Deposits a single instance of the specified item into the bank deposit box. If the instance is stackable like arrows
     * it will deposit all arrows into the bank.
     * <p>
     * This method performs different actions based on the origin of the container item:
     * <ul>
     *   <li>If the item's origin is {@literal @EQUIPMENT}, it interacts with the associated widget to deposit the item.</li>
     *   <li>Otherwise, it uses the interaction manager to execute a "Deposit-1" action for the item.</li>
     * </ul>
     *
     * @return {@code true} if the deposit operation is executed successfully, {@code false} otherwise.
     */
    public boolean deposit() {
        if(raw().getOrigin() == ContainerItem.ItemOrigin.EQUIPMENT) {
            return ctx.widgets().get(raw().getSlot()).interact(raw().getSlot(), -1, -1, 1);
        }
        return ctx.getInteractionManager().interact(raw, "Deposit-1");
    }

}