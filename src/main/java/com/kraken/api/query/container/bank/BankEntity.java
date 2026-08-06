package com.kraken.api.query.container.bank;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractEntity;
import com.kraken.api.service.bank.BankService;
import com.kraken.api.service.ui.UIService;
import com.kraken.api.service.util.SleepService;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.gameval.VarbitID;

@Slf4j
public class BankEntity extends AbstractEntity<BankItemWidget> {

    /** How long to wait for the server to acknowledge a note-mode toggle. */
    private static final long MODE_TIMEOUT_MS = 2_000L;

    /** How long to wait for a requested number-input dialogue to be drawn. */
    private static final long DIALOGUE_TIMEOUT_MS = 2_000L;

    public BankEntity(Context ctx, BankItemWidget raw) {
        super(ctx, raw);
    }

    @Override
    public String getName() {
        BankItemWidget item = raw();
        return item != null ? item.getName() : null;
    }

    @Override
    public boolean interact(String action) {
        BankItemWidget raw = raw();

        if (raw == null) return false;
        return ctx.getInteractionManager().interact(raw, action);
    }

    @Override
    public int getId() {
        BankItemWidget raw = raw();
        return raw != null ? raw.getItemId() : -1;
    }

    /**
     * Returns the count of this item in the bank.
     * @return The number of the item within the bank, or 0 if the item is absent.
     */
    public int count() {
        BankItemWidget raw = raw();
        return raw != null ? raw.getItemQuantity() : 0;
    }

    /**
     * Withdraws 1 of this item.
     * @return true if the withdrawal was successful and false otherwise
     */
    public boolean withdrawOne() {
        return interact("Withdraw-1");
    }

    /**
     * Withdraws 5 of this item.
     * @return true if the withdrawal was successful and false otherwise
     */
    public boolean withdrawFive() {
        return interact("Withdraw-5");
    }

    /**
     * Withdraws 10 of this item.
     * @return true if the withdrawal was successful and false otherwise
     */
    public boolean withdrawTen() {
        return interact("Withdraw-10");
    }

    /**
     * Withdraws All of this item.
     * @return true if the withdrawal was successful and false otherwise
     */
    public boolean withdrawAll() {
        return interact("Withdraw-All");
    }

    /**
     * Withdraws All of this item, ensuring it comes out as Notes.
     *
     * <p>Must be called off the client thread: the note-mode toggle is only reflected after the server
     * acknowledges it, and this waits for that acknowledgement before dispatching the withdraw.</p>
     *
     * @return true if the withdrawal was successful and false otherwise
     */
    public boolean withdrawAllNoted() {
        return setModeAndInteract(true, "Withdraw-All");
    }

    /**
     * Withdraws a specific amount. Handles "Withdraw-X" logic including scripts.
     * Defaults to un-noted (Item) mode.
     * @param amount The amount of the item to withdraw
     * @return true if the withdrawal was successful and false otherwise
     */
    public boolean withdraw(int amount) {
       return withdraw(amount, false);
    }

    /**
     * Withdraws a specific amount with explicit Note mode selection.
     *
     * <p>Must be called off the client thread: amounts outside 1/5/10 open a number dialogue that this
     * waits for before dismissing it.</p>
     *
     * @param amount The amount of the item to withdraw: 1, 5, or 10. Any other value will withdraw X of that value
     * @param noted True if the items should be withdrawn as notes and false if they should be withdrawn as items
     * @return true if the withdrawal was successful and false otherwise
     */
    public boolean withdraw(int amount, boolean noted) {
        BankItemWidget raw = raw();

        if (raw == null) return false;

        if (amount == 1) return setModeAndInteract(noted, "Withdraw-1");
        if (amount == 5) return setModeAndInteract(noted, "Withdraw-5");
        if (amount == 10) return setModeAndInteract(noted, "Withdraw-10");

        if (!ensureWithdrawMode(noted)) return false;

        boolean actionQueued = Boolean.TRUE.equals(ctx.runOnClientThread(() -> {
            int quantitySet = ctx.getVarbitValue(VarbitID.BANK_REQUESTEDQUANTITY);

            // The X value is already exactly what we need.
            if(quantitySet != amount) {
                ctx.getService(BankService.class).setXAmount(amount);
            }

            String action = quantitySet == amount ? "Withdraw-" + amount : "Withdraw-X";
            if (!ctx.getInteractionManager().interact(raw, action)) {
                return false;
            }

            ctx.getInteractionManager().getWidgetPackets().queueResumeCount(amount);
            return true;
        }));

        if (actionQueued && ctx.getVarbitValue(VarbitID.BANK_REQUESTEDQUANTITY) != amount) {
            // The count is answered by packet, but the dialogue the click opened is drawn only after
            // the server replies, and dismissing it has no effect until it exists. Wait for it here,
            // off the client thread.
            SleepService.sleepUntil(UIService::isNumberDialogueOpen, DIALOGUE_TIMEOUT_MS);
            UIService.closeNumberDialogue();
        }

        return actionQueued;
    }

    /**
     * Sets the bank withdrawal mode and then interacts. Helper method for withdrawing 1, 5, or 10 items.
     * @param noted True to withdraw as notes, false to withdraw as items
     * @param action The withdraw action to dispatch once the mode is confirmed
     * @return true if the withdrawal was successful and false otherwise
     */
    private boolean setModeAndInteract(boolean noted, String action) {
        return ensureWithdrawMode(noted) && interact(action);
    }

    /**
     * Puts the bank into the requested note mode and waits for the change to take effect.
     *
     * <p>{@code BankService.setWithdrawMode} dispatches the toggle click; the
     * {@code BANK_WITHDRAWNOTES} varbit reflects the new mode only once the server replies. The wait
     * happens here, off the client thread, so that callers can rely on the mode being active by the
     * time this returns.</p>
     *
     * @param noted True to withdraw as notes, false to withdraw as items
     * @return true once the bank is in the requested mode, false if the toggle failed or timed out
     */
    private boolean ensureWithdrawMode(boolean noted) {
        int targetMode = noted ? 1 : 0;
        if (ctx.getVarbitValue(VarbitID.BANK_WITHDRAWNOTES) == targetMode) {
            return true;
        }

        if (!Boolean.TRUE.equals(ctx.runOnClientThread(
                () -> ctx.getService(BankService.class).setWithdrawMode(noted)))) {
            log.warn("Failed to dispatch bank withdraw mode toggle (noted={})", noted);
            return false;
        }

        boolean applied = SleepService.sleepUntil(
                () -> ctx.getVarbitValue(VarbitID.BANK_WITHDRAWNOTES) == targetMode, MODE_TIMEOUT_MS);
        if (!applied) {
            log.warn("Timed out waiting for bank withdraw mode to change to noted={}", noted);
        }
        return applied;
    }
}
