package plugins.colosseum;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import plugins.colosseum.model.ColosseumState;
import plugins.colosseum.model.Modifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Singleton
@Slf4j
public class ColosseumStateTracker {

    private static final int REGION_LOBBY = 7316;
    private static final int REGION_COLOSSEUM = 7216;

    private static final int SCRIPT_MODIFIER_SELECT_INIT = 4931;
    private static final int VARBIT_MODIFIER_SELECTED = 9788;

    private static final ColosseumState DEFAULT_STATE = new ColosseumState(false, false, 1, false, Collections.emptyList());

    private final Client client;

    @Getter
    private ColosseumState currentState = DEFAULT_STATE;

    private int waveNumber = 1;
    private boolean waveStarted = false;

    @Getter
    private int waveStartTick;

    @Getter
    private final List<Modifier> modifierOptions = new ArrayList<>(3);

    @Getter(value = AccessLevel.PACKAGE, onMethod_ = @VisibleForTesting)
    private final List<Modifier> modifiers = new ArrayList<>(12);

    @Inject
    public ColosseumStateTracker(final Client client, final EventBus eventBus) {
        this.client = client;
        eventBus.register(this);
    }

    @Subscribe(priority = 5)
    public void onGameTick(GameTick e) {
        LocalPoint lp = client.getLocalPlayer().getLocalLocation();
        int region = lp == null ? -1 : WorldPoint.fromLocalInstance(client, lp).getRegionID();

        boolean inLobby = region == REGION_LOBBY;
        boolean inColosseum = client.getTopLevelWorldView().isInstance() && region == REGION_COLOSSEUM;

        if (!inColosseum) {
            waveNumber = 1;
            waveStarted = false;
            modifierOptions.clear();
            modifiers.clear();
        }

        setState(
                new ColosseumState(inLobby, inColosseum, waveNumber, waveStarted, Collections.unmodifiableList(modifiers)),
                false
        );
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged e) {
        switch (e.getGameState()) {
            case LOGGING_IN:
            case HOPPING:
                setState(DEFAULT_STATE, true);
        }
    }

    @Subscribe
    public void onChatMessage(ChatMessage e) {
        if (e.getType() != ChatMessageType.GAMEMESSAGE || !getCurrentState().isInColosseum()) {
            return;
        }

        String msg = e.getMessage();
        if (msg.contains("Sol Heredit jumps down from his seat")) {
            waveNumber = 12;
            waveStarted = true;
            waveStartTick = client.getTickCount();
        } else if (msg.contains("Wave: ")) {
            waveNumber = Integer.parseInt(msg.substring(18, msg.length() - 6));
            waveStarted = true;
            waveStartTick = client.getTickCount();
        } else if (msg.startsWith("Wave ") && msg.contains("completed!")) {
            // it's either a two-char number or a number and a space
            waveNumber = Integer.parseInt(msg.substring(5, 7).trim()) + 1;
            waveStarted = false;
        }
    }

    @Subscribe
    public void onNpcDespawned(NpcDespawned e) {
        if (currentState.isInColosseum() && e.getNpc().getId() == 12808) { // Minimus
            trackSelectedModifier();
        }
    }

    @Subscribe
    public void onScriptPreFired(ScriptPreFired e) {
        if (e.getScriptId() != SCRIPT_MODIFIER_SELECT_INIT) {
            return;
        }

        try {
            // pull the options available for next wave from the script args
            modifierOptions.clear();
            Object[] args = e.getScriptEvent().getArguments();
            modifierOptions.add(Modifier.forId((Integer) args[2]));
            modifierOptions.add(Modifier.forId((Integer) args[3]));
            modifierOptions.add(Modifier.forId((Integer) args[4]));
            log.debug("Modifier options = {}", modifierOptions);

            for (Modifier h : Modifier.forBitmask((Integer) args[8])) {
                if (!modifiers.contains(h)) {
                    modifiers.add(h);
                }
            }
        } catch (Exception ex) {
            log.warn("failed to extract modifier from arguments", ex);
        }
    }

    private void setState(ColosseumState newValue, boolean forceEvent) {
        if (!forceEvent && currentState.equals(newValue)) {
            return;
        }

        log.debug("Colosseum state change {} => {}", currentState, newValue);
        currentState = newValue;
    }

    private void trackSelectedModifier() {
        if (modifierOptions.isEmpty()) {
            log.warn("Wave started but modifier options were not tracked");
            return;
        }

        int selectedIx = client.getVarbitValue(VARBIT_MODIFIER_SELECTED);
        if (selectedIx == 0) {
            log.debug("varb {} = 0, no modifier selected?", VARBIT_MODIFIER_SELECTED);
            return;
        }

        Modifier selected = modifierOptions.get(selectedIx - 1);
        modifierOptions.clear();
        if (selected == null) {
            log.warn("Failed to select modifier with index {}, options = {}", selectedIx, modifierOptions);
            return;
        }

        if (!modifiers.contains(selected)) {
            modifiers.add(selected);
        }
        log.debug("Tracked modifier selection {} (ix {}), handicaps = {}", selected, selectedIx, modifiers);
    }
}
