package plugins.colosseum;

import com.google.inject.Inject;
import com.kraken.api.service.actor.ActorService;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.OverlayUtil;
import plugins.colosseum.model.spawns.Mob;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AutoColosseumNpcDebugOverlay extends Overlay {
    private final Client client;
    private final AutoColosseumPrayersPlugin plugin;
    private final AutoColosseumPrayersConfig config;

    @Inject
    public AutoColosseumNpcDebugOverlay(
            Client client,
            AutoColosseumPrayersPlugin plugin,
            AutoColosseumPrayersConfig config
    ) {
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
        setPriority(OverlayPriority.MED);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!config.showNpcLineOfSightDebug() && !config.showNpcPathingDebug()) {
            return null;
        }

        Player localPlayer = client.getLocalPlayer();
        if (localPlayer == null || client.getTopLevelWorldView() == null) {
            return null;
        }

        WorldPoint playerLocation = localPlayer.getWorldLocation();
        if (playerLocation == null) {
            return null;
        }

        List<NPC> colosseumNpcs = collectColosseumNpcs(playerLocation);
        int maxPathLength = Math.max(1, config.npcDebugPathLength());

        for (NPC npc : colosseumNpcs) {
            Mob mob = Mob.fromNpc(npc);
            if (mob == null) {
                continue;
            }

            Color base = colorForNpcId(npc.getId());
            Color losFill = alpha(base, 65);
            Color losBorder = alpha(base, 150);
            Color pathFill = alpha(base, 90);
            Color pathBorder = alpha(base, 220);

            boolean playerInLos = false;
            if (config.showNpcLineOfSightDebug()) {
                List<WorldPoint> losTiles = ActorService.getLineOfSightTiles(npc, Math.max(1, mob.getAttackRange()));
                renderTiles(graphics, losTiles, losFill, losBorder);
                playerInLos = losTiles.contains(playerLocation);
            }

            int pathLength = 0;
            if (config.showNpcPathingDebug()) {
                List<WorldPoint> path = config.npcPathStopOnLosDebug()
                        ? ActorService.getActorPathUntilLineOfSight(npc, localPlayer)
                        : ActorService.getActorPath(npc, localPlayer);

                if (path.size() > maxPathLength) {
                    path = new ArrayList<>(path.subList(0, maxPathLength));
                }

                renderPath(graphics, path, pathFill, pathBorder);
                pathLength = path.size();
            }

            if (config.showNpcDebugLabels()) {
                String npcName = npc.getName() == null ? "Unknown" : npc.getName();
                String label = npcName
                        + " idx=" + npc.getIndex()
                        + " los=" + (playerInLos ? "Y" : "N")
                        + " path=" + pathLength;
                net.runelite.api.Point textPoint = npc.getCanvasTextLocation(graphics, label, npc.getLogicalHeight() + 55);
                if (textPoint != null) {
                    OverlayUtil.renderTextLocation(graphics, textPoint, label, pathBorder);
                }
            }
        }

        return null;
    }

    private List<NPC> collectColosseumNpcs(WorldPoint playerLocation) {
        List<NPC> npcs = new ArrayList<>();
        if (!plugin.isInColosseum()) {
            return npcs;
        }

        for (NPC npc : client.getTopLevelWorldView().npcs()) {
            if (npc == null || npc.isDead()) {
                continue;
            }
            if (npc.getWorldLocation() == null) {
                continue;
            }
            if (Mob.fromNpc(npc) == null) {
                continue;
            }
            npcs.add(npc);
        }

        npcs.sort(Comparator.comparingInt(npc ->
                Math.max(
                        Math.abs(npc.getWorldLocation().getX() - playerLocation.getX()),
                        Math.abs(npc.getWorldLocation().getY() - playerLocation.getY())
                )));

        int maxNpcs = Math.max(1, config.npcDebugMaxNpcs());
        if (npcs.size() > maxNpcs) {
            return new ArrayList<>(npcs.subList(0, maxNpcs));
        }
        return npcs;
    }

    private void renderTiles(Graphics2D graphics, List<WorldPoint> tiles, Color fill, Color border) {
        for (WorldPoint tile : tiles) {
            LocalPoint localPoint = LocalPoint.fromWorld(client.getTopLevelWorldView(), tile);
            if (localPoint == null) {
                continue;
            }

            Polygon polygon = Perspective.getCanvasTilePoly(client, localPoint);
            if (polygon == null) {
                continue;
            }

            graphics.setColor(fill);
            graphics.fillPolygon(polygon);
            graphics.setColor(border);
            graphics.setStroke(new BasicStroke(1.2f));
            graphics.drawPolygon(polygon);
        }
    }

    private void renderPath(Graphics2D graphics, List<WorldPoint> path, Color fill, Color border) {
        net.runelite.api.Point previous = null;
        for (WorldPoint step : path) {
            LocalPoint localPoint = LocalPoint.fromWorld(client.getTopLevelWorldView(), step);
            if (localPoint == null) {
                continue;
            }

            Polygon polygon = Perspective.getCanvasTilePoly(client, localPoint);
            if (polygon != null) {
                graphics.setColor(fill);
                graphics.fillPolygon(polygon);
                graphics.setColor(border);
                graphics.setStroke(new BasicStroke(1.6f));
                graphics.drawPolygon(polygon);
            }

            net.runelite.api.Point center = Perspective.localToCanvas(client, localPoint, client.getTopLevelWorldView().getPlane());
            if (center != null && previous != null) {
                graphics.setColor(border);
                graphics.setStroke(new BasicStroke(1.5f));
                graphics.drawLine(previous.getX(), previous.getY(), center.getX(), center.getY());
            }
            previous = center;
        }
    }

    private Color colorForNpcId(int npcId) {
        float hue = ((npcId * 31) % 360) / 360.0f;
        return Color.getHSBColor(hue, 0.75f, 1.0f);
    }

    private Color alpha(Color color, int alpha) {
        int safeAlpha = Math.max(0, Math.min(255, alpha));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), safeAlpha);
    }
}
