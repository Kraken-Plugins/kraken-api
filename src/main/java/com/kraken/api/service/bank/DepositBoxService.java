package com.kraken.api.service.bank;

import com.google.inject.Provider;
import com.kraken.api.Context;
import com.kraken.api.query.equipment.EquipmentEntity;
import com.kraken.api.query.widget.WidgetEntity;
import com.kraken.api.service.dialogue.DialogueService;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.gameval.InterfaceID;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Service class for managing interactions with the bank deposit box interface within the client.
 *
 * <p>The {@code DepositBoxService} provides various methods to interact with
 * the deposit box interface, allowing users to manage their inventory, worn items,
 * and other specific interactions associated with the deposit functionality.</p>
 *
 * <p>All interactions assume the deposit box interface is open from the client's context.
 * Failing to meet this prerequisite may result in operation failure.</p>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *     <li>Checking the state of the deposit box interface (open/closed).</li>
 *     <li>Depositing all inventory items into the bank deposit box.</li>
 *     <li>Depositing worn items or specific worn equipment based on slot, item ID, or name.</li>
 *     <li>Depositing items from the looting bag into the deposit box.</li>
 * </ul>
 *
 * <p>The service utilizes a context provider ({@literal @ctxProvider}) to locate relevant
 * widgets and execute interaction commands within the game client. Proper widget interaction
 * ensures seamless deposit operations and the appropriate state handling for the deposit box interface.</p>
 */
@Singleton
public class DepositBoxService {

    @Inject
    private Provider<Context> ctxProvider;

    /**
     * Determines whether the bank deposit box interface is currently open.
     *
     * <p>This method verifies if the corresponding widget for the deposit box frame
     * is visible to the user within the client.</p>
     *
     * @return {@code true} if the deposit box interface is open; {@code false} otherwise.
     */
    public boolean isOpen() {
        WidgetEntity w = ctxProvider.get().widgets().fromClient(InterfaceID.BankDepositbox.FRAME);
        if(w == null) return false;
        return w.isVisible();
    }

    /**
     * Determines whether the bank deposit box interface is currently closed.
     *
     * <p>This method performs the inverse check of {@link #isOpen()} to determine
     * if the deposit box interface is not visible to the user.</p>
     *
     * @return {@code true} if the deposit box interface is closed; {@code false} otherwise.
     */
    public boolean isClosed() {
        return !isOpen();
    }

    /**
     * Closes the bank deposit box interface if it is currently open.
     *
     * <p>This method checks if the deposit box interface is already closed by
     * invoking {@link #isClosed()}. If the interface is open, it attempts to close
     * it by executing a client script.</p>
     *
     * <p>The operation will return {@code true} if the interface is successfully
     * verified as closed or was already closed before the method was invoked.</p>
     *
     * @return {@code true} if the deposit box interface is closed (either already closed or successfully closed);
     *         {@code false} if the operation cannot determine the state of the interface or failed to close it.
     */
    public boolean close() {
        if (isClosed()) {
            return true;
        }
        ctxProvider.get().runScript(29);
        return true;
    }

    /**
     * Deposits all items from the player's inventory into the bank deposit box.
     *
     * <p>This method interacts with the deposit box interface in the game client to move all
     * inventory items into the bank. It utilizes the {@literal @ctxProvider} to access the relevant
     * game widgets and send the interaction command.</p>
     *
     * <p>The method assumes that the deposit box interface is already open and accessible for
     * interaction. If the interface is not open, the operation may fail.</p>
     *
     * @return {@code true} if the interaction with the deposit box to deposit all items is
     *         successful; {@code false} otherwise.
     */
    public boolean depositAll() {
        return ctxProvider.get().widgets().fromClient(InterfaceID.BankDepositbox.DEPOSIT_INV)
                .interact(1, InterfaceID.BankDepositbox.DEPOSIT_INV, -1, -1);
    }

    /**
     * Deposits all worn items from the player's equipment into the bank deposit box.
     *
     * <p>This method interacts with the deposit box interface widget in the game client to move
     * all items currently equipped by the player into the bank. It uses the {@literal @ctxProvider}
     * to access the relevant widget component and send the interaction command.</p>
     *
     * <p>The method assumes that the deposit box interface is already open and accessible for interaction.
     * If the interface is not open, the operation may fail.</p>
     *
     * <p>The widget interaction is performed using the {@literal @BankDepositbox.DEPOSIT_WORN} constant
     * and invokes the interaction index 1 for depositing worn items.</p>
     *
     * @return {@code true} if the interaction was successfully executed and worn items were deposited;
     *         {@code false} otherwise if the interaction fails or conditions are not met.
     */
    public boolean depositWornItems() {
        return ctxProvider.get().widgets().fromClient(InterfaceID.BankDepositbox.DEPOSIT_WORN)
                .interact(1, InterfaceID.BankDepositbox.DEPOSIT_WORN, -1, -1);
    }

    /**
     * Deposits all items from the looting bag into the bank deposit box.
     *
     * <p>This method interacts with the specific widget representing the "Deposit Looting Bag" option
     * in the bank deposit box interface. It uses the {@literal @ctxProvider} to access the widget and
     * perform the interaction.</p>
     *
     * <ul>
     *     <li>The operation assumes that the bank deposit box interface is already open and accessible.</li>
     *     <li>If the interface is not open, the interaction may fail.</li>
     * </ul>
     *
     * @return {@code true} if the interaction with the "Deposit Looting Bag" widget is successful;
     *         {@code false} otherwise if the widget is not found or the interaction fails.
     */
    public boolean depositLootingBag() {
        return ctxProvider.get().widgets().fromClient(InterfaceID.BankDepositbox.DEPOSIT_LOOTINGBAG)
                .interact(1, InterfaceID.BankDepositbox.DEPOSIT_LOOTINGBAG, -1, -1);
    }


    /**
     * Deposits a specific worn equipment item from the player's inventory into the bank deposit box.
     *
     * <p>This method interacts with the deposit box interface to deposit a single worn
     * equipment item specified by its {@link EquipmentInventorySlot}. It uses the
     * {@literal @ctxProvider} to locate the corresponding widget and perform the interaction.</p>
     *
     * <ul>
     *     <li>The method interacts with the specific slot's widget, which corresponds to the
     *         player's equipped item to be deposited.</li>
     *     <li>The deposit box interface must be open and accessible for this operation to
     *         succeed. If the interface is not open, the interaction will fail.</li>
     * </ul>
     *
     * @param slot the {@link EquipmentInventorySlot} representing the specific item to be deposited.
     *             This parameter identifies the equipment slot (e.g., Helmet, Chest, Gloves) for
     *             the item to be interacted with.
     * @return {@code true} if the deposit operation on the specified worn item is successful;
     *         {@code false} if the interaction fails or the required conditions are not met
     *         (e.g., the deposit box is not open, or the widget is unavailable).
     */
    public boolean depositWorn(EquipmentInventorySlot slot) {
        return invokeDepositWorn(slot);
    }

    /**
     * Deposits a specific worn equipment item into the bank deposit box based on the item ID.
     *
     * <p>This method retrieves the worn equipment item associated with the given {@code id}
     * from the player's equipment. If the item is found and associated with a valid {@link EquipmentInventorySlot},
     * it attempts to deposit the item into the bank deposit box.</p>
     *
     * <ul>
     *     <li>The deposit operation is performed on the specific slot that corresponds
     *         to the equipment item with the provided {@code id}.</li>
     *     <li>The operation assumes the deposit box interface is already open; otherwise,
     *         the interaction may fail.</li>
     *     <li>If the item or its corresponding slot is not found, the method returns {@code false}.</li>
     * </ul>
     *
     * @param id the unique identifier of the equipment item to be deposited. This represents the
     *           item to locate in the player's worn equipment slots.
     * @return {@code true} if the interaction to deposit the worn item succeeds;
     *         {@code false} otherwise, such as when the item or slot is not found, or
     *         the deposit operation fails.
     */
    public boolean depositWorn(int id) {
        EquipmentEntity equipment = ctxProvider.get().equipment().inInterface().withId(id).first();
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
     * Deposits an item currently equipped on the player into a storage system.
     * <p>
     * This method attempts to locate an equipped item by its name and then identifies
     * its respective equipment slot. If both are valid, the item is deposited.
     * </p>
     *
     * @param name The name of the equipped item to be deposited.
     *             <p>
     *             This parameter specifies the exact name of the item as it appears
     *             in the game's interface or inventory system.
     *             </p>
     *
     * @return {@literal true} if the item was successfully deposited;
     *         {@literal false} if the item could not be located or deposited.
     */
    public boolean depositWorn(String name) {
        EquipmentEntity equipment = ctxProvider.get().equipment().inInterface().withName(name).first();
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
     * Attempts to invoke a deposit action for the worn item in the specified equipment slot.
     *
     * <p>This method identifies the widget associated with the provided equipment slot,
     * retrieves its widget ID, and performs an interaction to deposit the corresponding item.
     * The interaction is executed with a pre-defined action index and widget parameters.</p>
     *
     * @param slot The {@link EquipmentInventorySlot} representing the slot
     *             of the worn item to be deposited.
     *             <ul>
     *                <li>Must be a valid slot reference from the equipment inventory.</li>
     *                <li>Invalid or null slots might cause undesired behavior.</li>
     *             </ul>
     *
     * @return {@literal true} if the deposit interaction is successfully triggered;
     *         {@literal false} otherwise.
     */
    private boolean invokeDepositWorn(EquipmentInventorySlot slot) {
        int slotWidgetId = getDepositBoxWidget(slot);
        return ctxProvider.get().widgets().get(slotWidgetId).interact(2, slotWidgetId, -1, -1);
    }

    /**
     * Retrieves the interface widget ID corresponding to a deposit box slot
     * for the specified {@link EquipmentInventorySlot}.
     *
     * <p>This method maps equipment slots to their respective widget IDs used
     * in the deposit box interface. If the specified slot does not match
     * a known equipment slot, an {@link IllegalArgumentException} is thrown.</p>
     *
     * @param slot The {@link EquipmentInventorySlot} representing the equipment slot
     *             for which the deposit box widget ID is to be retrieved.
     * <p><ul>
     *     <li>{@literal @code HEAD} maps to {@literal @code SLOT0}</li>
     *     <li>{@literal @code CAPE} maps to {@literal @code SLOT1}</li>
     *     <li>{@literal @code AMULET} maps to {@literal @code SLOT2}</li>
     *     <li>{@literal @code WEAPON} maps to {@literal @code SLOT3}</li>
     *     <li>{@literal @code BODY} maps to {@literal @code SLOT4}</li>
     *     <li>{@literal @code SHIELD} maps to {@literal @code SLOT5}</li>
     *     <li>{@literal @code LEGS} maps to {@literal @code SLOT7}</li>
     *     <li>{@literal @code GLOVES} maps to {@literal @code SLOT9}</li>
     *     <li>{@literal @code BOOTS} maps to {@literal @code SLOT10}</li>
     *     <li>{@literal @code RING} maps to {@literal @code SLOT12}</li>
     *     <li>{@literal @code AMMO} maps to {@literal @code SLOT13}</li>
     * </ul></p>
     *
     * @return The interface widget ID as an integer that corresponds to the
     *         provided {@link EquipmentInventorySlot}.
     *
     * @throws IllegalArgumentException If an unknown or unsupported {@link EquipmentInventorySlot} is provided.
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
     * Sets the deposit quantity in the bank deposit box interface to the specified amount.
     *
     * <p>This method interacts with the game client's deposit box interface to set the quantity of items
     * to be deposited or withdrawn. It supports predefined options such as {@code 1}, {@code 5}, {@code 10},
     * and {@code ALL}, as well as a custom quantity input via numerical dialogue for other values.</p>
     *
     * <ul>
     *     <li>{@code 1}: Sets the quantity to deposit or withdraw 1 item.</li>
     *     <li>{@code 5}: Sets the quantity to deposit or withdraw 5 items.</li>
     *     <li>{@code 10}: Sets the quantity to deposit or withdraw 10 items.</li>
     *     <li>{@code ALL}: Deposits or withdraws all items.</li>
     *     <li>Other values: Prompts a numeric input dialogue to specify the desired quantity.</li>
     * </ul>
     *
     * <p>If the specified amount is not among the predefined options, the method invokes a dialogue service
     * to continue with a custom quantity input.</p>
     *
     * @param amount the quantity to set in the deposit box interface. Acceptable values are {@code 1}, {@code 5},
     *               {@code 10}, {@code -1} (for {@code ALL}), or any positive integer for custom input.
     * @return {@code true} if the operation to set the quantity or initiate the numerical dialogue is successful;
     *         {@code false} otherwise.
     */
    public boolean setQuantity(int amount) {
        Context ctx = ctxProvider.get();
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
