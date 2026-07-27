package plugins.colosseumv2;

import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.service.prayer.PrayerService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.HotkeyListener;
import plugins.colosseumv2.engine.*;
import plugins.colosseumv2.model.ColosseumState;
import plugins.colosseumv2.model.ColosseumStateChanged;
import plugins.colosseumv2.model.spawns.Mob;
import plugins.colosseumv2.overlay.ColosseumPrayerTabOverlay;
import plugins.colosseumv2.overlay.ColosseumSceneOverlay;
import plugins.colosseumv2.overlay.ColosseumStatusOverlay;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Automatic protection prayers for the Fortis Colosseum (waves 1-11 and reinforcements).
 *
 * <p>Division of responsibility: the player prays the first attack of any engagement (stepping
 * into line of sight, or a mob reaching them), because the plugin cannot know when the player
 * will commit. From the first observed attack onward the plugin tracks each mob's attack cycle
 * and keeps the correct protection prayer up, activating it while exactly one tick remains so
 * it is active before the attack animation's damage roll. The two exceptions are witnessed
 * manticore charges (plugin prays the whole first volley) and, optionally, jaguar warriors
 * whose pathing predicts an imminent melee hit.
 */
@Slf4j
@Singleton
@PluginDescriptor(
        name = "Auto Colo Prayers V2",
        description = "Automatically prays for you in the Colosseum.",
        tags = {"auto", "prayers", "colo", "colosseum"}
)
public class AutoColosseumPrayersV2Plugin extends Plugin {
    public static final String CONFIG_GROUP = "autocoloprayersv2";

    /** Injected for its side effect: constructing the Context loads the packet definitions
     * that {@link PrayerService} interactions depend on. */
    @Inject
    private Context ctx;

    @Inject
    private Client client;

    @Inject
    private PrayerService prayerService;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private KeyManager keyManager;

    @Inject
    private AutoColosseumPrayersV2Config config;

    @Inject
    private ColosseumStateTracker stateTracker;

    @Inject
    private ColosseumStatusOverlay statusOverlay;

    @Inject
    private ColosseumPrayerTabOverlay prayerTabOverlay;

    @Inject
    private ColosseumSceneOverlay sceneOverlay;

    @Getter
    private final PrayerEngine engine = new PrayerEngine();

    @Getter
    private long lastTickTime = -1;

    /** Prayer the engine wants active on the next tick, for overlays. */
    @Getter
    private Prayer activeTargetPrayer;

    /** Generic pre-pray prayer for the current wave, for overlays. */
    @Getter
    private Prayer prePrayPrayer;

    @Getter
    private PrayerDecision lastDecision = PrayerDecision.NONE;

    @Getter
    private boolean oneTickFlickEnabled;

    @Getter
    private boolean runtimeEnabled;

    /** Last prayer this plugin activated, so idle deactivation never fights the player. */
    private Prayer lastAppliedPrayer;

    private final Map<Integer, Integer> animationsChangedThisTick = new HashMap<>();
    private final Set<Integer> diedThisTick = new HashSet<>();

    private final HotkeyListener toggleListener = new HotkeyListener(() -> config.toggleHotkey()) {
        @Override
        public void hotkeyPressed() {
            runtimeEnabled = !runtimeEnabled;
            log.debug("Auto colosseum prayers toggled {}", runtimeEnabled ? "on" : "off");
        }
    };

    private final HotkeyListener flickListener = new HotkeyListener(() -> config.oneTickFlickHotkey()) {
        @Override
        public void hotkeyPressed() {
            oneTickFlickEnabled = !oneTickFlickEnabled;
            log.debug("One tick flick mode toggled {}", oneTickFlickEnabled ? "on" : "off");
        }
    };

    @Provides
    AutoColosseumPrayersV2Config provideConfig(final ConfigManager configManager) {
        return configManager.getConfig(AutoColosseumPrayersV2Config.class);
    }

    @Override
    protected void startUp() {
        runtimeEnabled = config.startEnabled();
        oneTickFlickEnabled = false;
        lastAppliedPrayer = null;
        engine.reset();

        keyManager.registerKeyListener(toggleListener);
        keyManager.registerKeyListener(flickListener);
        overlayManager.add(statusOverlay);
        overlayManager.add(prayerTabOverlay);
        overlayManager.add(sceneOverlay);
    }

    @Override
    protected void shutDown() {
        keyManager.unregisterKeyListener(toggleListener);
        keyManager.unregisterKeyListener(flickListener);
        overlayManager.remove(statusOverlay);
        overlayManager.remove(prayerTabOverlay);
        overlayManager.remove(sceneOverlay);
        engine.reset();
        animationsChangedThisTick.clear();
        diedThisTick.clear();
    }

    public ColosseumState getColosseumState() {
        return stateTracker.getCurrentState();
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event) {
        stateTracker.onGameStateChanged(event);
    }

    @Subscribe
    public void onChatMessage(ChatMessage event) {
        stateTracker.onChatMessage(event);
    }

    @Subscribe
    public void onScriptPreFired(ScriptPreFired event) {
        stateTracker.onScriptPreFired(event);
    }

    @Subscribe
    public void onNpcDespawned(NpcDespawned event) {
        stateTracker.onNpcDespawned(event);
    }

    @Subscribe
    public void onColosseumStateChanged(ColosseumStateChanged event) {
        boolean waveEnded = event.getPreviousState().isWaveStarted() && !event.getNewState().isWaveStarted();
        boolean leftColosseum = event.getPreviousState().isInColosseum() && !event.getNewState().isInColosseum();
        if (waveEnded || leftColosseum) {
            engine.reset();
            activeTargetPrayer = null;
            lastAppliedPrayer = null;
        }
    }

    @Subscribe
    public void onAnimationChanged(AnimationChanged event) {
        if (!(event.getActor() instanceof NPC)) {
            return;
        }
        NPC npc = (NPC) event.getActor();
        if (ColosseumConstants.TRACKED_NPC_IDS.contains(npc.getId())) {
            animationsChangedThisTick.put(npc.getIndex(), npc.getAnimation());
        }
    }

    @Subscribe
    public void onActorDeath(ActorDeath event) {
        if (event.getActor() instanceof NPC) {
            diedThisTick.add(((NPC) event.getActor()).getIndex());
        }
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        lastTickTime = System.currentTimeMillis();
        stateTracker.onGameTick();

        try {
            processTick();
        } catch (Exception e) {
            log.warn("Auto colosseum prayer tick failed", e);
        } finally {
            animationsChangedThisTick.clear();
            diedThisTick.clear();
        }
    }

    private void processTick() {
        ColosseumState state = stateTracker.getCurrentState();
        prePrayPrayer = ColosseumConstants.WAVE_PRE_PRAYER_MAP.get(state.getWaveNumber());

        if (!state.isInColosseum()) {
            if (engine.getTrackedCount() > 0) {
                engine.reset();
            }
            activeTargetPrayer = null;
            lastDecision = PrayerDecision.NONE;
            return;
        }

        Player player = client.getLocalPlayer();
        WorldView worldView = client.getTopLevelWorldView();
        if (player == null || worldView == null) {
            return;
        }

        int baseX = worldView.getBaseX();
        int baseY = worldView.getBaseY();
        WorldPoint playerLocation = player.getWorldLocation();

        LocalPoint lp = client.getLocalPlayer().getLocalLocation();
        int region = lp == null ? -1 : WorldPoint.fromLocalInstance(client, lp).getRegionID();

        boolean inColosseum = client.getTopLevelWorldView().isInstance() && region == ColosseumConstants.REGION_COLOSSEUM;

        TickInput.TickInputBuilder input = TickInput.builder()
                .tick(client.getTickCount())
                .playerX(playerLocation.getX() - baseX)
                .playerY(playerLocation.getY() - baseY)
                .collisionMap(SceneCollisionMap.capture(client))
                .inColosseum(inColosseum)
                .waveNumber(state.getWaveNumber())
                .waveStarted(state.isWaveStarted())
                .waveStartTick(state.getWaveStartTick())
                .prePrayPrayer(prePrayPrayer)
                .config(buildEngineConfig());

        for (NPC npc : worldView.npcs()) {
            if (npc == null || !ColosseumConstants.TRACKED_NPC_IDS.contains(npc.getId())) {
                continue;
            }

            WorldPoint location = npc.getWorldLocation();
            if (location == null) {
                continue;
            }

            NPCComposition composition = npc.getComposition();
            int size = composition != null && composition.getSize() > 0 ? composition.getSize() : 1;

            input.npc(NpcSnapshot.builder()
                    .index(npc.getIndex())
                    .npcId(npc.getId())
                    .name(npc.getName() == null ? "?" : npc.getName())
                    .mob(Mob.fromNpc(npc))
                    .sceneX(location.getX() - baseX)
                    .sceneY(location.getY() - baseY)
                    .size(size)
                    .animation(npc.getAnimation())
                    .animationChanged(animationsChangedThisTick.containsKey(npc.getIndex()))
                    .hasMageOrb(npc.hasSpotAnim(ColosseumConstants.GRAPHIC_MANTICORE_MAGE))
                    .hasRangeOrb(npc.hasSpotAnim(ColosseumConstants.GRAPHIC_MANTICORE_RANGE))
                    .hasMeleeOrb(npc.hasSpotAnim(ColosseumConstants.GRAPHIC_MANTICORE_MELEE))
                    .dead(npc.isDead() || diedThisTick.contains(npc.getIndex()))
                    .build());
        }

        PrayerDecision decision = engine.tick(input.build());
        lastDecision = decision;
        activeTargetPrayer = decision.getPrayer();

        applyDecision(decision);
    }

    private void applyDecision(PrayerDecision decision) {
        if (!runtimeEnabled || !config.enabled()) {
            return;
        }

        Prayer prayer = decision.getPrayer();
        if (prayer != null) {
            if (oneTickFlickEnabled) {
                prayerService.oneTickFlick(false, prayer);
            } else {
                prayerService.activatePrayer(prayer);
            }
            lastAppliedPrayer = prayer;
            return;
        }

        // No prayer needed next tick. Only ever deactivate the prayer this plugin itself put
        // up — a manually activated prayer (the player covering a first attack) is never
        // touched.
        boolean deactivate = oneTickFlickEnabled || config.deactivateOnIdle();
        if (deactivate && lastAppliedPrayer != null && prayerService.isActive(lastAppliedPrayer)) {
            prayerService.deactivatePrayer(lastAppliedPrayer);
            lastAppliedPrayer = null;
        }
    }

    private EngineConfig buildEngineConfig() {
        return EngineConfig.builder()
                .queueLookaheadTicks(config.queueLookaheadTicks())
                .cancelQueuedOnLosBreak(config.cancelQueuedOnLosBreak())
                .removeQueuedOnNpcDeath(config.removeQueuedOnNpcDeath())
                .prayJaguarOnPath(config.prayJaguarOnPath())
                .enableWavePrePray(config.enableWavePrePray())
                .prePrayWindowTicks(config.prePrayWindowTicks())
                .debugLosTiles(config.showNpcLineOfSightDebug())
                .debugPaths(config.showNpcPathingDebug() || config.showNpcDebugLabels())
                .debugStopPathOnLos(config.npcPathStopOnLosDebug())
                .debugMaxNpcs(config.npcDebugMaxNpcs())
                .debugPathLength(config.npcDebugPathLength())
                .build();
    }
}
