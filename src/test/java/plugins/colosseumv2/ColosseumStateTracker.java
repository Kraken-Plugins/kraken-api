package plugins.colosseumv2;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.util.Text;
import plugins.colosseumv2.model.ColosseumState;
import plugins.colosseumv2.model.ColosseumStateChanged;
import plugins.colosseumv2.model.Modifier;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tracks Fortis Colosseum run state (location, wave number, wave start tick, and the active
 * modifiers) and posts {@link ColosseumStateChanged} events on the RuneLite event bus.
 *
 * <p>Wave progression is chat driven: {@code "Wave: X"} marks a wave start, {@code "Wave X
 * completed!"} advances the wave counter, and Sol Heredit's entrance message marks wave 12.
 * Modifier state comes from the selection script's arguments plus the selected-modifier varbit
 * read when Minimus despawns.
 */
@Slf4j
@Singleton
public class ColosseumStateTracker {

    private static final Pattern WAVE_START_PATTERN = Pattern.compile("Wave: (\\d+)");
    private static final Pattern WAVE_COMPLETED_PATTERN = Pattern.compile("Wave (\\d+) completed!");

    @Inject
    private Client client;

    @Inject
    private EventBus eventBus;

    @Getter
    private ColosseumState currentState = ColosseumState.DEFAULT;

    private final List<Modifier> modifiers = new ArrayList<>();
    private final List<Modifier> modifierOptions = new ArrayList<>();

    /**
     * Re-evaluates the player's location. Call once per game tick.
     */
    public void onGameTick() {
        Player player = client.getLocalPlayer();
        if (player == null) {
            return;
        }

        WorldPoint location = WorldPoint.fromLocalInstance(client, player.getLocalLocation());
        int regionId = location == null ? -1 : location.getRegionID();
        boolean inLobby = regionId == ColosseumConstants.REGION_LOBBY;
        boolean inColosseum = regionId == ColosseumConstants.REGION_COLOSSEUM;

        ColosseumState state = currentState;
        if (inColosseum && !state.isInColosseum()) {
            setState(new ColosseumState(false, true, 1, false, -1, List.copyOf(modifiers)));
        } else if (!inColosseum && state.isInColosseum()) {
            reset(inLobby);
        } else if (inLobby != state.isInLobby()) {
            setState(new ColosseumState(inLobby, state.isInColosseum(), state.getWaveNumber(),
                    state.isWaveStarted(), state.getWaveStartTick(), List.copyOf(modifiers)));
        }
    }

    public void onGameStateChanged(GameStateChanged event) {
        if (event.getGameState() == GameState.LOGIN_SCREEN || event.getGameState() == GameState.HOPPING) {
            reset(false);
        }
    }

    public void onChatMessage(ChatMessage event) {
        if (event.getType() != ChatMessageType.GAMEMESSAGE || !currentState.isInColosseum()) {
            return;
        }

        String message = Text.removeTags(event.getMessage());
        ColosseumState state = currentState;

        if (message.contains(ColosseumConstants.CHAT_SOL_JUMPS_DOWN)) {
            setState(new ColosseumState(false, true, 12, true, client.getTickCount(), List.copyOf(modifiers)));
            return;
        }

        Matcher start = WAVE_START_PATTERN.matcher(message);
        if (message.startsWith(ColosseumConstants.CHAT_WAVE_PREFIX) && start.find()) {
            int wave = Integer.parseInt(start.group(1));
            setState(new ColosseumState(false, true, wave, true, client.getTickCount(), List.copyOf(modifiers)));
            return;
        }

        Matcher completed = WAVE_COMPLETED_PATTERN.matcher(message);
        if (message.contains(ColosseumConstants.CHAT_WAVE_COMPLETED) && completed.find()) {
            int wave = Integer.parseInt(completed.group(1));
            setState(new ColosseumState(false, true, wave + 1, false, state.getWaveStartTick(), List.copyOf(modifiers)));
        }
    }

    public void onScriptPreFired(ScriptPreFired event) {
        if (event.getScriptId() != ColosseumConstants.SCRIPT_MODIFIER_SELECT_INIT
                || event.getScriptEvent() == null) {
            return;
        }

        Object[] args = event.getScriptEvent().getArguments();
        if (args == null || args.length < 9) {
            return;
        }

        modifierOptions.clear();
        for (int i = 2; i <= 4; i++) {
            Modifier option = args[i] instanceof Number ? Modifier.forId(((Number) args[i]).intValue()) : null;
            modifierOptions.add(option);
        }

        if (args[8] instanceof Number) {
            int bitmask = ((Number) args[8]).intValue();
            for (Modifier active : Modifier.forBitmask(bitmask)) {
                addModifier(active);
            }
        }
    }

    public void onNpcDespawned(NpcDespawned event) {
        if (event.getNpc().getId() != ColosseumConstants.NPC_MINIMUS || !currentState.isInColosseum()) {
            return;
        }

        int selected = client.getVarbitValue(ColosseumConstants.VARBIT_MODIFIER_SELECTED);
        if (selected >= 1 && selected <= modifierOptions.size()) {
            Modifier chosen = modifierOptions.get(selected - 1);
            if (chosen != null) {
                addModifier(chosen);
            }
        }
    }

    private void addModifier(Modifier modifier) {
        if (modifier != null && !modifiers.contains(modifier)) {
            modifiers.add(modifier);
            ColosseumState state = currentState;
            setState(new ColosseumState(state.isInLobby(), state.isInColosseum(), state.getWaveNumber(),
                    state.isWaveStarted(), state.getWaveStartTick(), List.copyOf(modifiers)));
        }
    }

    private void reset(boolean inLobby) {
        modifiers.clear();
        modifierOptions.clear();
        setState(new ColosseumState(inLobby, false, 1, false, -1, List.of()));
    }

    private void setState(ColosseumState newState) {
        ColosseumState previous = currentState;
        if (previous.equals(newState) && previous.getModifiers().size() == newState.getModifiers().size()) {
            return;
        }

        currentState = newState;
        log.debug("Colosseum state changed: {}", newState);
        eventBus.post(new ColosseumStateChanged(previous, newState));
    }
}
