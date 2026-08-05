package plugins.api.precondition;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.query.gameobject.GameObjectEntity;
import com.kraken.api.query.npc.NpcEntity;
import com.kraken.api.service.bank.BankService;
import com.kraken.api.service.bank.DepositBoxService;
import com.kraken.api.service.dialogue.DialogueService;
import lombok.extern.slf4j.Slf4j;
import plugins.api.ApiTestConfig;
import plugins.api.suite.CancellationToken;

/**
 * Opens and closes the banking interfaces on the engine's behalf.
 *
 * <p>Five tests carried their own copy of the same fourteen lines of bank-booth clicking, each
 * slightly different — some searched by object name, one by NPC action, and they disagreed about how
 * long to wait and whether to check the result. This is the single implementation they collapse into.
 *
 * <p>Banks are resolved by the {@code "Bank"} menu action rather than by object name, which covers
 * booths, chests and banker NPCs uniformly — including the Grand Exchange, where there is no booth at
 * all.</p>
 *
 * <p>Everything here blocks and must be called off the client thread.</p>
 */
@Slf4j
@Singleton
public class BankHelper {

    private static final long INTERFACE_TIMEOUT_MS = 8000;
    private static final int OPEN_ATTEMPTS = 3;
    private static final long ATTEMPT_SETTLE_MS = 1500;

    @Inject
    private Context ctx;

    @Inject
    private BankService bankService;

    @Inject
    private DepositBoxService depositBoxService;

    @Inject
    private DialogueService dialogueService;

    @Inject
    private ApiTestConfig config;

    @Inject
    private Waiter waiter;

    /**
     * Opens the bank, entering the configured PIN if the keypad appears.
     *
     * @param token polled throughout so a cancelled run stops promptly
     * @return true when the bank interface is open
     */
    public boolean open(CancellationToken token) {
        if (bankService.isOpen()) {
            return true;
        }

        for (int attempt = 1; attempt <= OPEN_ATTEMPTS; attempt++) {
            token.throwIfCancelled("opening the bank");

            if (!clickNearestBank()) {
                log.error("Nothing offering a 'Bank' action is nearby");
                return false;
            }

            waiter.until(() -> bankService.isOpen() || bankService.isPinOpen(),
                    ATTEMPT_SETTLE_MS, token, "the bank to respond");

            if (bankService.isPinOpen() && !enterConfiguredPin(token)) {
                return false;
            }

            if (waiter.until(bankService::isOpen, INTERFACE_TIMEOUT_MS, token, "the bank to open")) {
                return true;
            }

            log.warn("Bank did not open on attempt {} of {}", attempt, OPEN_ATTEMPTS);
        }

        return false;
    }

    /**
     * Closes the bank interface.
     *
     * @param token polled throughout so a cancelled run stops promptly
     * @return true when the bank interface is closed
     */
    public boolean close(CancellationToken token) {
        if (bankService.isClosed()) {
            return true;
        }

        bankService.close();
        return waiter.until(bankService::isClosed, INTERFACE_TIMEOUT_MS, token, "the bank to close");
    }

    /**
     * Opens the nearest bank deposit box.
     *
     * @param token polled throughout so a cancelled run stops promptly
     * @return true when the deposit box interface is open
     */
    public boolean openDepositBox(CancellationToken token) {
        if (depositBoxService.isOpen()) {
            return true;
        }

        GameObjectEntity box = ctx.gameObjects().withAction("Deposit").nearest();
        if (box == null || box.isNull()) {
            log.error("No bank deposit box nearby");
            return false;
        }

        box.interact("Deposit");
        return waiter.until(depositBoxService::isOpen, INTERFACE_TIMEOUT_MS, token,
                "the deposit box to open");
    }

    /**
     * Closes the deposit box interface.
     *
     * @param token polled throughout so a cancelled run stops promptly
     * @return true when the deposit box interface is closed
     */
    public boolean closeDepositBox(CancellationToken token) {
        if (depositBoxService.isClosed()) {
            return true;
        }

        depositBoxService.close();
        return waiter.until(depositBoxService::isClosed, INTERFACE_TIMEOUT_MS, token,
                "the deposit box to close");
    }

    /**
     * Best effort sweep that dismisses any interface a previous test may have left open.
     *
     * <p>This is driven by observation rather than by declared side effects on purpose: a test that
     * threw halfway through can leave any interface open, and no declaration covers that case. Never
     * throws, because it runs in cleanup paths where a secondary failure would mask the real one.</p>
     *
     * @param token polled throughout so a cancelled run stops promptly
     */
    public void closeEverything(CancellationToken token) {
        try {
            if (dialogueService.isDialoguePresent()) {
                // Continue rather than walk the tree: we only need the modal gone.
                dialogueService.continueDialogue();
                waiter.sleep(600, token);
            }

            close(token);
            closeDepositBox(token);
        } catch (Exception e) {
            log.warn("Could not fully close open interfaces", e);
        }
    }

    /**
     * Clicks the nearest thing offering a bank action, preferring an object over an NPC.
     *
     * @return true when something was clicked
     */
    private boolean clickNearestBank() {
        GameObjectEntity booth = ctx.gameObjects().withAction("Bank").nearest();
        if (booth != null && booth.isPresent()) {
            booth.interact("Bank");
            return true;
        }

        NpcEntity banker = ctx.npcs().withAction("Bank").nearest();
        if (banker != null && banker.isPresent()) {
            banker.interact("Bank");
            return true;
        }

        return false;
    }

    /**
     * Types the PIN configured in the plugin settings into the bank keypad.
     *
     * @param token polled throughout so a cancelled run stops promptly
     * @return true when the keypad was dismissed
     */
    private boolean enterConfiguredPin(CancellationToken token) {
        int[] pin = parsePin(config.bankPin());
        if (pin == null) {
            log.error("A bank pin is required but none is configured. Set it in the plugin settings, "
                    + "or pre-enter the pin before starting the run");
            return false;
        }

        if (!bankService.enterPin(pin)) {
            log.error("Failed to enter the configured bank pin");
            return false;
        }

        return waiter.until(() -> !bankService.isPinOpen(), INTERFACE_TIMEOUT_MS, token,
                "the bank pin keypad to close");
    }

    /**
     * Parses a four digit PIN from configuration.
     *
     * <p>Rejects anything that is not exactly four ASCII digits rather than attempting a partial
     * parse. Typing garbage at the keypad is expensive — repeated wrong entries lock the bank — so a
     * malformed value must surface as a skipped test with a clear reason instead.</p>
     *
     * <p>Public and static so it can be unit tested without a client.</p>
     *
     * @param raw the configured value, which may be null or blank
     * @return the four digits, or null when the value is missing or malformed
     */
    public static int[] parsePin(String raw) {
        if (raw == null) {
            return null;
        }

        String trimmed = raw.trim();
        if (trimmed.length() != 4) {
            return null;
        }

        int[] digits = new int[4];
        for (int i = 0; i < 4; i++) {
            char character = trimmed.charAt(i);
            if (character < '0' || character > '9') {
                return null;
            }
            digits[i] = character - '0';
        }

        return digits;
    }
}
