package plugins.colosseum;

import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import plugins.colosseum.overlay.AutoColosseumInfoOverlay;
import plugins.colosseum.overlay.AutoColosseumSceneOverlay;
import plugins.colosseum.simulation.Scratch;
import plugins.colosseum.simulation.State;
import plugins.colosseum.simulation.Tick;
import plugins.colosseum.simulation.LoadoutConfig;
import plugins.colosseum.simulation.PlayerCommand;
import plugins.colosseum.simulation.live.Capture;
import plugins.colosseum.simulation.live.Executor;
import plugins.colosseum.simulation.live.WaveTracker;
import plugins.colosseum.simulation.plan.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Debug and automation harness for the colosseum simulation engine: captures the arena
 * every game tick, runs the time-budgeted planner, renders the evidence (tile scores,
 * danger map, planned path, predicted NPC movement, reasoning) and optionally executes
 * the decision through the Kraken API.
 */
@Slf4j
@Singleton
@PluginDescriptor(
        name = "Auto Colosseum",
        description = "Real-time decision search for the Fortis Colosseum with debug visualization.",
        tags = {"kraken", "colosseum", "simulation", "planner"}
)
public class AutoColosseumPlugin extends Plugin {
    @Inject
    private OverlayManager overlayManager;

    @Inject
    private AutoColosseumConfig config;

    @Inject
    private AutoColosseumSceneOverlay sceneOverlay;

    @Inject
    private AutoColosseumInfoOverlay infoOverlay;

    @Inject
    private Context context;

    @Inject
    private Executor executor;

    @Getter
    private final WaveTracker tracker = new WaveTracker();

    @Getter
    private final Planner planner = new Planner();

    @Getter
    private final Scratch previewScratch = new Scratch();

    @Getter
    private final State previewState = new State();

    @Getter
    private LoadoutConfig loadout = LoadoutConfig.defaults();

    @Getter
    private volatile Capture lastCapture;

    @Getter
    private volatile Decision lastDecision;

    @Getter
    private volatile String lastError;

    /** Predicted NPC paths (world points) for the overlay, keyed by RuneLite npc index. */
    @Getter
    private final Map<Integer, List<WorldPoint>> npcPredictedPaths = new HashMap<>();

    private int currentGearSet;

    /** Configured weapon ids per gear set (melee, ranged, magic), for set detection. */
    private final int[][] weaponIdsBySet = new int[3][];

    @Provides
    AutoColosseumConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(AutoColosseumConfig.class);
    }

    @Override
    protected void startUp() {
        applyConfig();
        overlayManager.add(sceneOverlay);
        overlayManager.add(infoOverlay);
    }

    @Override
    protected void shutDown() {
        overlayManager.remove(sceneOverlay);
        overlayManager.remove(infoOverlay);
        tracker.reset();
        lastCapture = null;
        lastDecision = null;
        npcPredictedPaths.clear();
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event) {
        if (!"krakenColosseum".equals(event.getGroup())) {
            return;
        }
        if ("copyWeaponButton".equals(event.getKey())) {
            copyToClipboard(equippedIdsCsv(true));
            return;
        }
        if ("copyGearButton".equals(event.getKey())) {
            copyToClipboard(equippedIdsCsv(false));
            return;
        }
        applyConfig();
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event) {
        if (event.getGameState() != GameState.LOGGED_IN) {
            tracker.reset();
            lastCapture = null;
            lastDecision = null;
            npcPredictedPaths.clear();
        }
    }

    @Subscribe
    public void onNpcSpawned(NpcSpawned event) {
        tracker.onNpcSpawned(event.getNpc());
    }

    @Subscribe
    public void onNpcDespawned(NpcDespawned event) {
        tracker.onNpcDespawned(event.getNpc());
    }

    @Subscribe
    public void onAnimationChanged(AnimationChanged event) {
        tracker.onAnimationChanged(event.getActor(), context.getClient().getLocalPlayer());
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        if (!config.enabled()) {
            return;
        }
        try {
            tracker.onGameTick();
            detectCurrentGearSet();
            Capture capture = Capture.capture(context, tracker, loadout, currentGearSet);
            lastCapture = capture;
            if (capture == null || capture.getFrame().getNpcSlotCount() == 0) {
                lastDecision = null;
                npcPredictedPaths.clear();
                return;
            }

            Decision decision = planner.plan(capture.getState(), plannerOptions());
            lastDecision = decision;
            predictNpcPaths(capture);

            if (config.autoExecute()) {
                if (decision.getGearSetIndex() >= 0) {
                    currentGearSet = decision.getGearSetIndex();
                }
                executor.execute(decision, loadout);
            }
            lastError = null;
        } catch (Exception ex) {
            lastError = ex.getMessage();
            log.debug("Colosseum planner tick failed", ex);
        }
    }

    private PlannerOptions plannerOptions() {
        PlannerOptions.PlannerOptionsBuilder builder = PlannerOptions.builder()
                .budgetNanos(config.budgetMillis() * 1_000_000L)
                .stage2HorizonTicks(config.stageTwoHorizonTicks())
                .stage2TopPlans(config.stageTwoTopPlans())
                .horizonTicks(config.horizonTicks())
                .dangerRadius(config.dangerRadius())
                .maxDestinations(config.maxDestinations())
                .maxSafeTiles(config.maxSafeTiles())
                .maxAttackTiles(config.maxAttackTiles())
                .maxTargets(config.maxTargets())
                .sipBrewAtHp(config.sipBrew())
                .sipRestoreAtPrayer(config.sipRestore())
                .eatComboAtHp(config.eatComboAt())
                        .scorer(Scorer.builder()
                                .deathPenalty(config.deathPenalty())
                                .lethalRiskPerHp(config.lethalRiskPerHp())
                                .hpWeight(config.hpWeight())
                                .damageTakenWeight(config.damageTakenWeight())
                                .killWeight(config.killWeight())
                                .damageDealtWeight(config.damageDealtWeight())
                                .supplyWeight(config.supplyWeight())
                                .prayerWeight(config.prayerWeight())
                                .endExposureWeight(config.endExposureWeight())
                                .runEnergyWeight(config.runEnergyWeight()).build());
        builder.eatFoodAtHp(config.eatFoodAtHp());
        return builder.build();
    }

    private void applyConfig() {
        tracker.setManticoreRangedSpotAnim(2683);
        tracker.setManticoreMagicSpotAnim(2681);
        rebuildLoadout();
    }

    /**
     * Rebuilds the loadout's gear sets from the configured weapon/gear ids. The weapon ids
     * are kept separately so the current set can be detected from the equipped weapon; the
     * executor equips weapon ids first so weapon speed changes land before armour.
     */
    private void rebuildLoadout() {
        int[][] weapons = {
                parseIds(config.meleeWeaponIds()),
                parseIds(config.rangedWeaponIds()),
                parseIds(config.magicWeaponIds()),
        };
        int[][] gear = {
                parseIds(config.meleeGearIds()),
                parseIds(config.rangedGearIds()),
                parseIds(config.magicGearIds()),
        };
        LoadoutConfig defaults = LoadoutConfig.defaults();
        LoadoutConfig.GearSet[] sets = new LoadoutConfig.GearSet[defaults.gearSetCount()];
        for (int i = 0; i < sets.length; i++) {
            LoadoutConfig.GearSet base = defaults.gearSet(i);
            if (i < weapons.length) {
                weaponIdsBySet[i] = weapons[i];
                sets[i] = base.toBuilder().equipItemIds(concat(weapons[i], gear[i])).build();
            } else {
                sets[i] = base;
            }
        }
        loadout = defaults.toBuilder().gearSets(sets).build();
    }

    /**
     * Detects the gear set the player currently wears by matching the equipped weapon
     * against the configured per-style weapon ids. Runs on the client thread (game tick).
     */
    private void detectCurrentGearSet() {
        ItemContainer worn = context.getClient().getItemContainer(InventoryID.WORN);
        if (worn == null) {
            return;
        }
        Item weapon = worn.getItem(EquipmentInventorySlot.WEAPON.getSlotIdx());
        if (weapon == null) {
            return;
        }
        for (int set = 0; set < weaponIdsBySet.length; set++) {
            if (weaponIdsBySet[set] == null) {
                continue;
            }
            for (int id : weaponIdsBySet[set]) {
                if (id == weapon.getId()) {
                    currentGearSet = set;
                    return;
                }
            }
        }
    }

    /**
     * @param weaponOnly true for the weapon slot only, false for everything except it.
     * @return comma-separated ids of the requested equipped items.
     */
    private String equippedIdsCsv(boolean weaponOnly) {
        String csv = context.runOnClientThread(() -> {
            ItemContainer worn = context.getClient().getItemContainer(InventoryID.WORN);
            if (worn == null) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            for (EquipmentInventorySlot slot : EquipmentInventorySlot.values()) {
                boolean isWeaponSlot = slot == EquipmentInventorySlot.WEAPON;
                if (weaponOnly != isWeaponSlot) {
                    continue;
                }
                Item item = worn.getItem(slot.getSlotIdx());
                if (item == null || item.getId() < 0) {
                    continue;
                }
                if (sb.length() > 0) {
                    sb.append(',');
                }
                sb.append(item.getId());
            }
            return sb.toString();
        });
        return csv == null ? "" : csv;
    }

    private void copyToClipboard(String text) {
        try {
            StringSelection selection = new StringSelection(text == null ? "" : text);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
        } catch (Exception e) {
            log.error("failed to copy to clipboard: {}", e.getMessage());
        }
    }

    private static int[] parseIds(String csv) {
        if (csv == null || csv.trim().isEmpty()) {
            return new int[0];
        }
        String[] tokens = csv.split("[,;\\s]+");
        int[] parsed = new int[tokens.length];
        int count = 0;
        for (String token : tokens) {
            if (token.isEmpty()) {
                continue;
            }
            try {
                parsed[count++] = Integer.parseInt(token.trim());
            } catch (NumberFormatException ignored) {
                // Skip malformed entries and keep the rest.
            }
        }
        int[] trimmed = new int[count];
        System.arraycopy(parsed, 0, trimmed, 0, count);
        return trimmed;
    }

    private static int[] concat(int[] first, int[] second) {
        int[] merged = new int[first.length + second.length];
        System.arraycopy(first, 0, merged, 0, first.length);
        System.arraycopy(second, 0, merged, first.length, second.length);
        return merged;
    }

    /**
     * Predicts NPC movement over the next few ticks by advancing a copy of the captured
     * state with no player input, recording each NPC's anchor per tick.
     */
    private void predictNpcPaths(Capture capture) {
        npcPredictedPaths.clear();
        if (!config.showNpcPaths()) {
            return;
        }
        previewState.copyFrom(capture.getState());
        int slotCount = capture.getFrame().getNpcSlotCount();
        short[] lastPos = new short[slotCount];
        for (int slot = 0; slot < slotCount; slot++) {
            lastPos[slot] = previewState.npcPos(slot);
        }
        int ticks = Math.max(2, config.npcPathTicks());
        for (int t = 0; t < ticks; t++) {
            Tick.advance(previewState, PlayerCommand.NONE, previewScratch);
            for (int slot = 0; slot < slotCount; slot++) {
                if (!previewState.npcActive(slot)) {
                    continue;
                }
                short pos = previewState.npcPos(slot);
                if (pos == lastPos[slot]) {
                    continue;
                }
                lastPos[slot] = pos;
                int npcIndex = capture.getFrame().getNpcRuneliteIndices()[slot];
                npcPredictedPaths
                        .computeIfAbsent(npcIndex, k -> new ArrayList<>())
                        .add(capture.getFrame().getGrid().toWorld(pos));
            }
        }
    }
}
