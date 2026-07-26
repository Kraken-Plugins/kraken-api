package plugins.colosseumv2.overlay;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;
import plugins.colosseumv2.AutoColosseumPrayersV2Config;
import plugins.colosseumv2.AutoColosseumPrayersV2Plugin;
import plugins.colosseumv2.engine.PrayerDecision;
import plugins.colosseumv2.engine.PrayerEngine;
import plugins.colosseumv2.engine.QueuedAttack;
import plugins.colosseumv2.model.ColosseumState;
import plugins.colosseumv2.model.Modifier;
import plugins.colosseumv2.model.spawns.WaveSpawn;
import plugins.colosseumv2.model.spawns.WaveSpawns;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.List;

/**
 * Status panel: runtime on/off state, the prayer targeted for the next tick, upcoming queue
 * rows, and (optionally) expanded wave/state diagnostics.
 */
@Singleton
public class ColosseumStatusOverlay extends OverlayPanel {

    private static final Color ACTIVE = new Color(0, 200, 83);
    private static final Color INACTIVE = new Color(244, 67, 54);
    private static final Color PRE_PRAY = new Color(0, 188, 212);
    private static final Color QUEUE = new Color(200, 200, 200);
    private static final Color DEBUG = new Color(160, 160, 160);
    private static final Color WARN = new Color(255, 152, 0);

    private final AutoColosseumPrayersV2Plugin plugin;
    private final AutoColosseumPrayersV2Config config;
    private final Client client;

    @Inject
    public ColosseumStatusOverlay(AutoColosseumPrayersV2Plugin plugin, AutoColosseumPrayersV2Config config, Client client) {
        this.plugin = plugin;
        this.config = config;
        this.client = client;
        setPosition(OverlayPosition.TOP_LEFT);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!config.showStatusOverlay()) {
            return null;
        }

        ColosseumState state = plugin.getColosseumState();
        if (!state.isInColosseum() && !state.isInLobby()) {
            return null;
        }

        boolean on = plugin.isRuntimeEnabled() && config.enabled();
        panelComponent.getChildren().add(TitleComponent.builder()
                .text("Colo Prayers")
                .color(on ? ACTIVE : INACTIVE)
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Status")
                .right(on ? "ACTIVE" : "OFF")
                .rightColor(on ? ACTIVE : INACTIVE)
                .build());

        if (plugin.isOneTickFlickEnabled()) {
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("1-Tick Flick")
                    .right("ON")
                    .rightColor(WARN)
                    .build());
        }

        PrayerDecision decision = plugin.getLastDecision();
        String next = "-";
        Color nextColor = QUEUE;
        if (decision.getPrayer() != null) {
            next = prayerName(decision);
            nextColor = decision.getReason() == PrayerDecision.Reason.PRE_PRAY ? PRE_PRAY : ACTIVE;
        }
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Next Tick")
                .right(next)
                .rightColor(nextColor)
                .build());

        PrayerEngine engine = plugin.getEngine();

        if (config.showQueueOverlay()) {
            int tick = engine.getCurrentTick();
            int shown = 0;
            for (QueuedAttack attack : engine.getQueueView()) {
                if (shown++ >= config.queueOverlayLines()) {
                    break;
                }

                StringBuilder right = new StringBuilder(attack.getStyleLabel());
                if (attack.isArtilleryLikely()) {
                    right.append(" [art]");
                }
                if (attack.isJaguarPriority()) {
                    right.append(" [jag]");
                }

                panelComponent.getChildren().add(LineComponent.builder()
                        .left("+" + (attack.getLandTick() - tick) + "t " + shortName(attack.getNpcName()))
                        .leftColor(QUEUE)
                        .right(right.toString())
                        .rightColor(styleColor(attack.getStyleLabel()))
                        .build());
            }
        }

        if (config.expandDebugOverlay()) {
            addDebugLines(state, engine);
        }

        return super.render(graphics);
    }

    private void addDebugLines(ColosseumState state, PrayerEngine engine) {
        int tick = client.getTickCount();

        panelComponent.getChildren().add(TitleComponent.builder()
                .text("— Debug —")
                .color(DEBUG)
                .build());

        addDebug("Wave", String.valueOf(state.getWaveNumber()));
        addDebug("Wave Started", state.isWaveStarted()
                ? "yes (+" + (tick - state.getWaveStartTick()) + "t)"
                : "no");
        addDebug("Wave Start Tick", String.valueOf(state.getWaveStartTick()));
        addDebug("Client Tick", String.valueOf(tick));
        addDebug("Tracked NPCs", String.valueOf(engine.getTrackedCount()));
        addDebug("Queue Size", String.valueOf(engine.getQueueView().size()));
        addDebug("Pre-Pray", plugin.getPrePrayPrayer() == null ? "-" : plugin.getPrePrayPrayer().name());

        PrayerDecision decision = plugin.getLastDecision();
        if (decision.getContested() > 1) {
            addDebug("Contested", decision.getContested() + " same-tick attacks");
        }

        List<Modifier> modifiers = state.getModifiers();
        if (!modifiers.isEmpty()) {
            StringBuilder names = new StringBuilder();
            for (Modifier modifier : modifiers) {
                if (names.length() > 0) {
                    names.append(", ");
                }
                names.append(modifier.getName(1));
            }
            addDebug("Modifiers", names.toString());
        }

        try {
            WaveSpawns spawns = state.isWaveStarted()
                    ? state.getWaveSpawns(client)
                    : null;
            WaveSpawns next = state.getWaveNumber() < 12 ? state.getNextWaveSpawns(client) : null;
            if (spawns != null) {
                addDebug("Wave Spawns", describeSpawns(spawns));
            }
            if (next != null) {
                addDebug("Next Wave", describeSpawns(next));
            }
        } catch (Exception ignored) {
            // Spawn preview is best-effort only.
        }

        for (String line : engine.describeManticores()) {
            panelComponent.getChildren().add(LineComponent.builder()
                    .left(line)
                    .leftColor(WARN)
                    .build());
        }
    }

    private String describeSpawns(WaveSpawns spawns) {
        StringBuilder text = new StringBuilder();
        for (WaveSpawn spawn : spawns.getSpawns()) {
            if (text.length() > 0) {
                text.append(", ");
            }
            text.append(spawn.getCount()).append('x').append(shortMob(spawn.getEnemy()));
        }
        for (WaveSpawn spawn : spawns.getReinforcements()) {
            if (text.length() > 0) {
                text.append(", ");
            }
            text.append('+').append(spawn.getCount()).append('x').append(shortMob(spawn.getEnemy()));
        }
        return text.toString();
    }

    private String shortMob(plugins.colosseumv2.model.spawns.Mob mob) {
        switch (mob) {
            case FREMENIK_WARBAND:
                return "Frem";
            case SERPENT_SHAMAN:
            case REINFORCEMENT_SHAMAN:
                return "Shaman";
            case JAVELIN_COLOSSUS:
                return "Jav";
            case JAGUAR_WARRIOR:
                return "Jag";
            case MANTICORE:
                return "Manti";
            case SHOCKWAVE_COLOSSUS:
                return "Shock";
            case MINOTAUR:
                return "Mino";
            case SOL_HEREDIT:
                return "Sol";
            default:
                return mob.name();
        }
    }

    private void addDebug(String left, String right) {
        panelComponent.getChildren().add(LineComponent.builder()
                .left(left)
                .leftColor(DEBUG)
                .right(right)
                .rightColor(DEBUG)
                .build());
    }

    private String prayerName(PrayerDecision decision) {
        String base;
        switch (decision.getPrayer()) {
            case PROTECT_FROM_MAGIC:
                base = "Mage";
                break;
            case PROTECT_FROM_MISSILES:
                base = "Range";
                break;
            case PROTECT_FROM_MELEE:
                base = "Melee";
                break;
            default:
                base = decision.getPrayer().name();
                break;
        }

        if (decision.getReason() == PrayerDecision.Reason.PRE_PRAY) {
            return base + " (pre)";
        }
        if (decision.getWinner() != null) {
            return base + " (" + shortName(decision.getWinner().getNpcName()) + ")";
        }
        return base;
    }

    private String shortName(String name) {
        if (name == null) {
            return "?";
        }
        return name.length() > 16 ? name.substring(0, 16) : name;
    }

    private Color styleColor(String styleLabel) {
        if ("Mage".equals(styleLabel)) {
            return new Color(60, 160, 255);
        }
        if ("Range".equals(styleLabel)) {
            return new Color(60, 200, 80);
        }
        if ("Melee".equals(styleLabel)) {
            return new Color(255, 90, 80);
        }
        return QUEUE;
    }
}
