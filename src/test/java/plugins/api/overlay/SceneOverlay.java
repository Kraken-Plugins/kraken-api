package plugins.api.overlay;

import com.google.inject.Inject;
import com.kraken.api.Context;
import com.kraken.api.query.gameobject.GameObjectEntity;
import com.kraken.api.query.groundobject.GroundObjectEntity;
import com.kraken.api.query.npc.NpcEntity;
import com.kraken.api.query.player.LocalPlayerEntity;
import com.kraken.api.query.player.PlayerEntity;
import com.kraken.api.query.widget.WidgetEntity;
import com.kraken.api.service.actor.ActorService;
import com.kraken.api.service.pathfinding.LocalPathfinder;
import com.kraken.api.service.tile.GameArea;
import com.kraken.api.service.tile.TileService;
import plugins.api.ApiTestConfig;
import plugins.api.ApiTestPlugin;
import plugins.api.tests.service.AreaServiceTest;
import net.runelite.api.Actor;
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.*;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static net.runelite.client.ui.overlay.OverlayUtil.renderPolygon;


public class SceneOverlay extends Overlay {
    private final ApiTestPlugin plugin;
    private final LocalPathfinder pathfinder;
    private final Context ctx;
    private final ApiTestConfig config;
    private final TileService tileService;
    private final AreaServiceTest areaServiceTest;
    private final Map<Integer, Integer> npcManualRangeOverrides = new HashMap<>();
    private JFrame npcRangeEditorFrame;
    private JTable npcRangeEditorTable;
    private DefaultTableModel npcRangeEditorModel;
    private long lastNpcRangeUiRefresh;

    @Inject
    public SceneOverlay(ApiTestPlugin plugin, LocalPathfinder pathfinder, Context ctx, ApiTestConfig config,
                        TileService tileService, AreaServiceTest areaServiceTest) {
        this.plugin = plugin;
        this.pathfinder = pathfinder;
        this.ctx = ctx;
        this.config = config;
        this.tileService = tileService;
        this.areaServiceTest = areaServiceTest;

        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
        setPriority(OverlayPriority.MED);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        List<WorldPoint> path = plugin.getCurrentPath();
        renderTargetTile(graphics);

        if(config.renderCurrentPath()) {
            pathfinder.renderPath(path, graphics, Color.GREEN);
            pathfinder.renderPath(plugin.getScriptPath(), graphics, new Color(8, 166, 236));
            pathfinder.renderPath(plugin.getPathfinderTestPath(), graphics, new Color(255, 215, 0));
            pathfinder.renderMinimapPath(path, graphics, Color.CYAN);
            if(plugin.getTargetArea() != null) {
                renderArea(plugin.getTargetArea(), graphics);
            }
        }

        if (config.showGameObjects()) {
            renderGameObjects(graphics);
        }

        if(config.showGroundObjects()) {
            renderGroundItems(graphics);
        }

        if(config.showNpcs()) {
            renderNpcs(graphics);
        }

        if(config.showPlayers()) {
            renderOtherPlayers(graphics);
        }

        if(config.showSelf()) {
            renderLocalPlayer(graphics);
        }

        if(config.showDebugInfo()) {
            renderApiDebug(graphics);
        }

        if(config.showWidgetDebug()) {
            renderWidgetDebug(graphics);
        }

        if(config.showAreaService()) {
            renderAreaService(graphics);
        }

        if(config.showNpcLoS()) {
            renderNpcLoS(graphics);
        } else {
            hideNpcRangeEditor();
        }

        if (config.showNpcPathing()) {
            renderNpcPathing(graphics);
        }

        return null;
    }

    private void renderNpcPathing(Graphics2D g) {
        Player localPlayer = ctx.getClient().getLocalPlayer();
        if (localPlayer == null) {
            return;
        }

        WorldPoint playerLocation = localPlayer.getWorldLocation();
        if (playerLocation == null) {
            return;
        }

        int scanRange = Math.max(1, config.npcPathScanRange());
        List<NPC> nearbyNpcs = ctx.npcs()
                .attackable()
                .toRuneLite()
                .filter(Objects::nonNull)
                .filter(npc -> npc.getWorldLocation() != null && npc.getWorldLocation().distanceTo2D(playerLocation) <= scanRange)
                .collect(Collectors.toList());

        for (NPC npc : nearbyNpcs) {
            List<WorldPoint> path = config.npcPathStopOnLos()
                    ? ActorService.getActorPathUntilLineOfSight(npc, localPlayer)
                    : ActorService.getActorPath(npc, localPlayer);

            Color baseColor = config.npcPathUsePerNpcColors()
                    ? colorForNpcId(npc.getId())
                    : config.npcPathColor();
            Color fillColor = withAlpha(baseColor, config.npcPathFillAlpha());
            Color borderColor = withAlpha(baseColor, config.npcPathBorderAlpha());

            if (!path.isEmpty()) {
                renderPathTiles(g, path, fillColor, borderColor);
            }

            if (config.npcPathShowTerminationTile()) {
                WorldPoint terminationTile;
                if (config.npcPathStopOnLos()) {
                    terminationTile = path.isEmpty()
                            ? ActorService.getActorLineOfSightTerminationTile(npc, localPlayer)
                            : path.get(path.size() - 1);
                } else {
                    terminationTile = path.isEmpty() ? npc.getWorldLocation() : path.get(path.size() - 1);
                }

                renderPathTerminationTile(g, terminationTile, fillColor, borderColor);
            }

            if (config.showDebugInfo()) {
                String name = npc.getName() == null ? "Unknown" : npc.getName();
                String debug = String.format("%s path=%d", name, path.size());
                net.runelite.api.Point text = npc.getCanvasTextLocation(g, debug, npc.getLogicalHeight() + 75);
                if (text != null) {
                    OverlayUtil.renderTextLocation(g, text, debug, borderColor);
                }
            }
        }
    }

    private void renderPathTiles(Graphics2D graphics, List<WorldPoint> path, Color fillColor, Color borderColor) {
        net.runelite.api.Point previousCenter = null;

        for (WorldPoint point : path) {
            LocalPoint localPoint = LocalPoint.fromWorld(ctx.getClient().getTopLevelWorldView(), point);
            if (localPoint == null) {
                continue;
            }

            Polygon polygon = Perspective.getCanvasTilePoly(ctx.getClient(), localPoint);
            if (polygon != null) {
                graphics.setColor(fillColor);
                graphics.fillPolygon(polygon);
                graphics.setColor(borderColor);
                graphics.drawPolygon(polygon);
            }

            net.runelite.api.Point center = Perspective.localToCanvas(
                    ctx.getClient(),
                    localPoint,
                    ctx.getClient().getTopLevelWorldView().getPlane()
            );
            if (center != null && previousCenter != null) {
                graphics.setColor(borderColor);
                graphics.setStroke(new BasicStroke(1f));
                graphics.drawLine(previousCenter.getX(), previousCenter.getY(), center.getX(), center.getY());
            }
            previousCenter = center;
        }
    }

    private void renderPathTerminationTile(Graphics2D graphics, WorldPoint tile, Color fillColor, Color borderColor) {
        if (tile == null) {
            return;
        }

        LocalPoint localPoint = LocalPoint.fromWorld(ctx.getClient().getTopLevelWorldView(), tile);
        if (localPoint == null) {
            return;
        }

        Polygon polygon = Perspective.getCanvasTilePoly(ctx.getClient(), localPoint);
        if (polygon == null) {
            return;
        }

        graphics.setColor(fillColor);
        graphics.fillPolygon(polygon);
        graphics.setColor(borderColor);
        graphics.setStroke(new BasicStroke(2f));
        graphics.drawPolygon(polygon);
    }

    private void renderNpcLoS(Graphics2D g) {
        Player localPlayer = ctx.getClient().getLocalPlayer();
        if (localPlayer == null) {
            return;
        }

        WorldPoint playerLocation = localPlayer.getWorldLocation();
        if (playerLocation == null) {
            return;
        }

        int scanRange = Math.max(1, config.npcLoSScanRange());
        List<NPC> nearbyNpcs = ctx.npcs()
                .attackable()
                .toRuneLite()
                .filter(Objects::nonNull)
                .filter(npc -> npc.getWorldLocation() != null && npc.getWorldLocation().distanceTo2D(playerLocation) <= scanRange)
                .collect(Collectors.toList());

        if (config.showNpcLoSRangeEditor()) {
            updateNpcRangeEditor(nearbyNpcs);
        } else {
            hideNpcRangeEditor();
        }

        for (NPC npc : nearbyNpcs) {
            int range = resolveNpcRange(npc);
            if (range <= 0) {
                continue;
            }

            List<WorldPoint> losTiles = ActorService.getLineOfSightTiles(npc, range);
            if (losTiles.isEmpty()) {
                continue;
            }

            Color baseColor = config.npcLoSUsePerNpcColors()
                    ? colorForNpcId(npc.getId())
                    : config.npcLoSColor();
            Color fillColor = withAlpha(baseColor, config.npcLoSFillAlpha());
            Color borderColor = withAlpha(baseColor, config.npcLoSBorderAlpha());

            for (WorldPoint tile : losTiles) {
                LocalPoint localPoint = LocalPoint.fromWorld(ctx.getClient().getTopLevelWorldView(), tile);
                if (localPoint == null) {
                    continue;
                }

                Polygon polygon = Perspective.getCanvasTilePoly(ctx.getClient(), localPoint);
                if (polygon == null) {
                    continue;
                }

                g.setColor(fillColor);
                g.fillPolygon(polygon);
                g.setColor(borderColor);
                g.drawPolygon(polygon);
            }

            if (config.showDebugInfo()) {
                String debug = String.format("%s r=%d", npc.getName(), range);
                net.runelite.api.Point text = npc.getCanvasTextLocation(g, debug, npc.getLogicalHeight() + 60);
                if (text != null) {
                    OverlayUtil.renderTextLocation(g, text, debug, borderColor);
                }
            }
        }
    }

    private int resolveNpcRange(NPC npc) {
        int manualRange = npcManualRangeOverrides.getOrDefault(npc.getId(), 0);
        if (manualRange > 0) {
            return manualRange;
        }

        if (config.npcLoSUseDetectedRanges()) {
            int detectedRange = detectNpcRange(npc);
            if (detectedRange > 0) {
                return detectedRange;
            }
        }

        return Math.max(1, config.npcLoSDefaultRange());
    }

    private int detectNpcRange(NPC npc) {
        if (npc.getComposition() == null) {
            return -1;
        }

        // RL composition int param 13 usually tracks attack range for many NPCs.
        int detected = npc.getComposition().getIntValue(13);
        if (detected > 0 && detected <= 25) {
            return detected;
        }

        String[] actions = npc.getComposition().getActions();
        if (actions == null) {
            return -1;
        }

        for (String action : actions) {
            if ("attack".equalsIgnoreCase(action)) {
                return 1;
            }
        }

        return -1;
    }

    private void updateNpcRangeEditor(List<NPC> nearbyNpcs) {
        long now = System.currentTimeMillis();
        if (now - lastNpcRangeUiRefresh < 750) {
            return;
        }
        lastNpcRangeUiRefresh = now;

        List<Object[]> rows = new ArrayList<>();
        for (NPC npc : nearbyNpcs) {
            if (npc == null || npc.getComposition() == null) {
                continue;
            }

            int npcId = npc.getId();
            String name = npc.getName() == null ? "Unknown" : npc.getName();
            int detectedRange = detectNpcRange(npc);
            Integer override = npcManualRangeOverrides.get(npcId);
            rows.add(new Object[]{
                    npcId,
                    name,
                    detectedRange > 0 ? detectedRange : "",
                    override == null ? "" : override
            });
        }
        rows.sort((a, b) -> Integer.compare((Integer) a[0], (Integer) b[0]));

        SwingUtilities.invokeLater(() -> {
            ensureNpcRangeEditor();
            npcRangeEditorModel.setRowCount(0);
            for (Object[] row : rows) {
                npcRangeEditorModel.addRow(row);
            }
        });
    }

    private void ensureNpcRangeEditor() {
        if (npcRangeEditorFrame != null) {
            if (!npcRangeEditorFrame.isVisible()) {
                npcRangeEditorFrame.setVisible(true);
            }
            return;
        }

        npcRangeEditorModel = new DefaultTableModel(new Object[]{"Npc ID", "Name", "Detected", "Manual"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 3;
            }
        };

        npcRangeEditorModel.addTableModelListener(event -> {
            if (event.getType() != TableModelEvent.UPDATE || event.getColumn() != 3) {
                return;
            }

            int row = event.getFirstRow();
            if (row < 0 || row >= npcRangeEditorModel.getRowCount()) {
                return;
            }

            Object idValue = npcRangeEditorModel.getValueAt(row, 0);
            Object manualValue = npcRangeEditorModel.getValueAt(row, 3);
            if (!(idValue instanceof Integer)) {
                return;
            }

            int npcId = (Integer) idValue;
            int manualRange = parseManualRange(manualValue);
            if (manualRange <= 0) {
                npcManualRangeOverrides.remove(npcId);
            } else {
                npcManualRangeOverrides.put(npcId, manualRange);
            }
        });

        npcRangeEditorTable = new JTable(npcRangeEditorModel);
        npcRangeEditorTable.setFillsViewportHeight(true);

        npcRangeEditorFrame = new JFrame("NPC LoS Range Editor");
        npcRangeEditorFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        npcRangeEditorFrame.add(new JScrollPane(npcRangeEditorTable), BorderLayout.CENTER);
        npcRangeEditorFrame.setSize(420, 360);
        npcRangeEditorFrame.setLocationByPlatform(true);
        npcRangeEditorFrame.setVisible(true);
    }

    private void hideNpcRangeEditor() {
        if (npcRangeEditorFrame == null || !npcRangeEditorFrame.isVisible()) {
            return;
        }
        SwingUtilities.invokeLater(() -> {
            if (npcRangeEditorFrame != null) {
                npcRangeEditorFrame.setVisible(false);
            }
        });
    }

    private int parseManualRange(Object value) {
        if (value == null) {
            return -1;
        }

        String text = value.toString().trim();
        if (text.isEmpty()) {
            return -1;
        }

        try {
            int parsed = Integer.parseInt(text);
            return (parsed > 0 && parsed <= 25) ? parsed : -1;
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private Color colorForNpcId(int npcId) {
        float hue = ((npcId * 37) % 360) / 360f;
        return Color.getHSBColor(hue, 0.75f, 1.0f);
    }

    private Color withAlpha(Color color, int alpha) {
        int safeAlpha = Math.max(0, Math.min(255, alpha));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), safeAlpha);
    }

    private void renderAreaService(Graphics2D graphics) {
        if (areaServiceTest.reachableArea != null) {
            Color reachColor = new Color(0, 255, 0, 80);
            areaServiceTest.reachableArea.render(ctx.getClient(), graphics, reachColor, false);
            areaServiceTest.reachableArea.render(ctx.getClient(), graphics, Color.GREEN, true);

            GameArea area = areaServiceTest.reachableArea;
            if (!area.getTiles().isEmpty()) {
                WorldPoint first = area.getTiles().iterator().next();
                LocalPoint lp = LocalPoint.fromWorld(ctx.getClient(), first);
                if (lp != null) {
                    net.runelite.api.Point p = Perspective.getCanvasTextLocation(ctx.getClient(), graphics, lp, "Reachable", 0);
                    if (p != null) OverlayUtil.renderTextLocation(graphics, p, "Reachable", Color.GREEN);
                }
            }
        }

        if(areaServiceTest.radiusArea != null) {
            Color radiusColor = new Color(255, 0, 0, 80);
            areaServiceTest.radiusArea.render(ctx.getClient(), graphics, radiusColor, false);
            areaServiceTest.radiusArea.render(ctx.getClient(), graphics, Color.RED, true);
        }

        if (areaServiceTest.polygonArea != null) {
            Color polyColor = new Color(255, 0, 255, 100);
            areaServiceTest.polygonArea.render(ctx.getClient(), graphics, polyColor, false);
            areaServiceTest.polygonArea.render(ctx.getClient(), graphics, Color.MAGENTA, true);
        }
    }

    /**
     * Renders a Target area with a solid border and transparent fill.
     * @param area WorldArea the area to render
     */
    private void renderArea(WorldArea area, Graphics2D graphics) {
        if(area != null) {
            int centerX = area.getX() + area.getWidth() / 2;
            int centerY = area.getY() + area.getHeight() / 2;
            WorldPoint center = new WorldPoint(centerX, centerY, area.getPlane());
            LocalPoint centerPt = LocalPoint.fromWorld(ctx.getClient().getTopLevelWorldView(), center);
            if(centerPt == null) {
                return;
            }

            Polygon polygon = Perspective.getCanvasTileAreaPoly(ctx.getClient(), centerPt, area.getWidth(), area.getHeight(), area.getPlane(), 0);
            if (polygon != null) {
                graphics.setColor(new Color(0, 255, 255, 0));
                graphics.fill(polygon);
                graphics.setColor(Color.CYAN); // Solid Cyan
                graphics.draw(polygon);
            }
        }
    }

    private void renderTargetTile(Graphics2D g) {
        if(plugin.getTargetTile() != null) {
            LocalPoint lp;
            if(ctx.getClient().getTopLevelWorldView().isInstance()) {
                lp = tileService.fromWorldInstance(plugin.getTargetTile());
            } else {
                lp = LocalPoint.fromWorld(ctx.getClient().getTopLevelWorldView(), plugin.getTargetTile());
            }

            if(lp == null) return;
            Polygon polygon = Perspective.getCanvasTilePoly(ctx.getClient(), lp);
            if(polygon == null) return;

            renderPolygon(g, polygon, new Color(241, 160, 9), new Color(241, 160, 9, 20), new BasicStroke(2));
        }
    }

    private void renderLocalPlayer(Graphics2D graphics) {
        LocalPlayerEntity localEntity = ctx.players().local();
        if (localEntity != null && localEntity.raw() != null) {

            Actor interacting = localEntity.raw().getInteracting();
            Color color = Color.BLUE;
            String status = localEntity.isMoving() ? "Moving" : "Idle";

            if (interacting != null) {
                if (localEntity.raw().isInteracting()) {
                    color = Color.YELLOW;
                    status = "Interacting";
                }
            }

            String text = String.format("%s (Lvl: %d) | %s",
                    localEntity.getName(),
                    localEntity.raw().getCombatLevel(),
                    status
            );

            renderPlayerPolygon(graphics, localEntity.raw(), color, text);
        }
    }

    private void renderOtherPlayers(Graphics2D graphics) {
        List<PlayerEntity> players = ctx.players()
                .withinDistance(config.playerRange())
                .list();

        for (PlayerEntity p : players) {
            Color color = Color.WHITE;
            String status = "Idle";

            Actor interacting = p.raw().getInteracting();

            if (interacting != null) {
                if (interacting == ctx.getClient().getLocalPlayer()) {
                    color = Color.RED; // Interacting with ME (Warning)
                    status = "Targeting Me";
                } else {
                    color = Color.YELLOW; // Interacting with someone else
                    status = "Busy";
                }
            }

            String text = String.format("%s (Lvl: %d) | %s",
                    p.getName(),
                    p.raw().getCombatLevel(),
                    status
            );

            renderPlayerPolygon(graphics, p.raw(), color, text);
        }
    }

    private void renderWidgetDebug(Graphics2D graphics) {
        net.runelite.api.Point mouse = ctx.getClient().getMouseCanvasPosition();

        // 2. Find the top-most visible widget under the mouse
        // We filter for visible widgets, then check bounds manually or use API utils if available.
        // Since we want to test the Query API, let's use it to narrow down candidates,
        // though strictly 'widgets under point' is complex due to layering.
        // A simple approach for debugging is iterating visible widgets.
        WidgetEntity hovered = ctx.widgets().visible().stream()
                .filter(w -> {
                    Rectangle bounds = w.raw().getBounds();
                    return bounds != null && bounds.contains(mouse.getX(), mouse.getY());
                })
                // Sort by area size (smallest first) usually gives the specific button
                // rather than the container, or use depth logic if available.
                .min((w1, w2) -> {
                    Rectangle r1 = w1.raw().getBounds();
                    Rectangle r2 = w2.raw().getBounds();
                    return Double.compare(r1.getWidth() * r1.getHeight(), r2.getWidth() * r2.getHeight());
                })
                .orElse(null);


        if(hovered == null) {
            return;
        }

        Widget w = hovered.raw();
        Rectangle bounds = w.getBounds();

        // Highlight
        graphics.setColor(Color.MAGENTA);
        graphics.draw(bounds);

        // Info Panel
        int x = bounds.x + bounds.width + 5;
        int y = bounds.y;

        // Ensure info panel stays on screen
        if (x + 150 > ctx.getClient().getCanvasWidth()) {
            x = bounds.x - 155;
        }

        String[] info = new String[] {
                "ID: " + w.getId() + " (" + (w.getId() >> 16) + ":" + (w.getId() & 0xFFFF) + ")",
                "Index: " + w.getIndex(),
                "Text: " + w.getText(),
                "Name: " + w.getName(),
                "Actions: " + (w.getActions() != null ? String.join(", ", w.getActions()) : "null"),
                "Sprite: " + w.getSpriteId()
        };

        // Draw background for text
        graphics.setColor(new Color(0, 0, 0, 180));
        graphics.fillRect(x, y, 200, info.length * 15 + 5);

        graphics.setColor(Color.WHITE);
        for (int i = 0; i < info.length; i++) {
            graphics.drawString(info[i], x + 5, y + 15 + (i * 15));
        }
    }

    private void renderApiDebug(Graphics2D graphics) {
        // Debug 1: visualize the result of .nearest()
        // This draws a line from local player to the result of ctx.players().nearest()
        PlayerEntity nearest = ctx.players().nearest();

        if (nearest != null && !nearest.isNull()) {
            LocalPoint start = ctx.getClient().getLocalPlayer().getLocalLocation();
            LocalPoint end = nearest.raw().getLocalLocation();

            if (start != null && end != null) {
                net.runelite.api.Point p1 = Perspective.localToCanvas(ctx.getClient(), start, ctx.getClient().getTopLevelWorldView().getPlane());
                net.runelite.api.Point p2 = Perspective.localToCanvas(ctx.getClient(), end, ctx.getClient().getTopLevelWorldView().getPlane());

                if (p1 != null && p2 != null) {
                    graphics.setColor(Color.CYAN);
                    graphics.drawLine(p1.getX(), p1.getY(), p2.getX(), p2.getY());
                    OverlayUtil.renderTextLocation(graphics, new net.runelite.api.Point((p1.getX()+p2.getX())/2, (p1.getY()+p2.getY())/2), "Nearest", Color.CYAN);
                }
            }
        }

        // Debug 2: Count players interacting with me
        long targetingMe = ctx.players().interactingWith(ctx.getClient().getLocalPlayer()).stream().count();
        if (targetingMe > 0) {
            OverlayUtil.renderTextLocation(graphics, new net.runelite.api.Point(30, 30), "WARNING: " + targetingMe + " players targeting you!", Color.RED);
        }
    }

    private void renderPlayerPolygon(Graphics2D graphics, Player entity, Color color, String label) {
        if (entity == null) return;

        Shape poly = entity.getConvexHull();
        if (poly != null) {
            graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 100));
            graphics.fill(poly);
            graphics.setColor(color);
            graphics.draw(poly);
        }

        net.runelite.api.Point textLoc = entity.getCanvasTextLocation(graphics, label, entity.getLogicalHeight() + 40);
        if (textLoc != null) {
            OverlayUtil.renderTextLocation(graphics, textLoc, label, color);
        }
    }

    private void renderGameObjects(Graphics2D graphics) {
        for(GameObjectEntity entity : ctx.gameObjects().within(config.gameObjectRange()).interactable().list()) {
            LocalPoint playerLoc = ctx.players().local().localLocation();
            LocalPoint objLoc = entity.raw().getLocalLocation();

            int distance = playerLoc.distanceTo(objLoc) / Perspective.LOCAL_TILE_SIZE;
            boolean isReachable = ctx.getService(TileService.class).isObjectReachable(entity.raw());

            String[] rawActions = entity.getObjectComposition().getActions();
            String actionString = "[]";
            if (rawActions != null) {
                actionString = Arrays.toString(Arrays.stream(rawActions)
                        .filter(s -> s != null && !s.isEmpty())
                        .toArray());
            }

            String overlayText = String.format("%s | Dist: %d | R: %b | %s",
                    entity.getName(),
                    distance,
                    isReachable,
                    actionString);

            net.runelite.api.Point textLocation = entity.raw().getCanvasTextLocation(graphics, overlayText, 0);

            if (textLocation != null) {
                Color textColor = isReachable ? Color.GREEN : Color.RED;

                OverlayUtil.renderTextLocation(graphics, textLocation, overlayText, textColor);

                if (entity.raw().getClickbox() != null) {
                    OverlayUtil.renderPolygon(graphics, entity.raw().getClickbox(), textColor);
                }
            }
        }
    }

    /**
     * Renders all nearby NPCs with color coding based on their state.
     */
    private void renderNpcs(Graphics2D graphics) {
        // Use your API to get all valid NPCs within 15 tiles
        List<NpcEntity> nearbyNpcs = ctx.npcs().within(config.npcRange()).stream().collect(Collectors.toList());

        for (NpcEntity npcWrapper : nearbyNpcs) {
            NPC npc = npcWrapper.raw();

            Color color;
            String status = "Idle";

            boolean isReachable = ctx.getService(TileService.class).isTileReachable(npc.getWorldLocation());
            boolean isDead = npc.isDead();
            Actor interacting = npc.getInteracting();

            List<String> actions = Arrays.stream(npc.getComposition().getActions())
                    .filter(java.util.Objects::nonNull)
                    .map(String::toLowerCase)
                    .collect(Collectors.toList());
            boolean isAttackable = actions.contains("attack") && !isDead;

            if (interacting == ctx.getClient().getLocalPlayer()) {
                color = Color.RED;
                status = "Aggro";
            } else if (!isReachable) {
                color = Color.GRAY; // Unreachable
                status = "Unreachable";
            } else if (interacting != null) {
                color = Color.YELLOW; // Busy interacting with someone else
                status = "Busy";
            } else if (isAttackable) {
                color = Color.GREEN; // Ready to fight
                status = "Attackable";
            } else {
                color = Color.CYAN; // Default/Idle
            }

            Shape clickbox = npc.getConvexHull();
            if (clickbox != null) {
                graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 100));
                graphics.fill(clickbox);
                graphics.setColor(color);
                graphics.draw(clickbox);
            }

            // Format: Name (Lvl: 10) | [Attack, Talk] | Status
            String actionString = actions.isEmpty() ? "[]" : actions.toString();
            String text = String.format("%s (Lvl: %d) | %s",
                    npc.getName(),
                    npc.getCombatLevel(),
                    status);

            net.runelite.api.Point textLoc = npc.getCanvasTextLocation(graphics, text, npc.getLogicalHeight() + 40);
            if (textLoc != null) {
                OverlayUtil.renderTextLocation(graphics, textLoc, text, color);

                // Draw secondary line for actions if needed
                net.runelite.api.Point actionLoc = new net.runelite.api.Point(textLoc.getX(), textLoc.getY() + 15);
                OverlayUtil.renderTextLocation(graphics, actionLoc, actionString, Color.LIGHT_GRAY);
            }
        }
    }

    private void renderGroundItems(Graphics2D graphics) {
        for(GroundObjectEntity entity : ctx.groundItems().within(config.groundObjectRange()).stream().collect(Collectors.toList())) {
            String name = entity.getName();
            int qty = entity.raw().getQuantity();
            int gePrice = entity.raw().getGePrice() * qty;
            int haPrice = entity.raw().getHaPrice() * qty;

            boolean isReachable = ctx.getService(TileService.class).isTileReachable(entity.raw().getLocation());

            // Format: Name (Qty) | GE: 100 | HA: 50
            StringBuilder sb = new StringBuilder();
            sb.append(name);
            if (qty > 1) {
                sb.append("(").append(qty).append(")");
            }
            sb.append(" | GE: ").append(formatValue(gePrice));
            sb.append(" | HA: ").append(formatValue(haPrice));

            Color textColor = Color.WHITE;
            if (!isReachable) {
                textColor = Color.RED;
            } else if (gePrice > 10000) {
                textColor = new Color(217, 5, 250);
            }

            LocalPoint pt = LocalPoint.fromWorld(ctx.getClient().getTopLevelWorldView(), entity.raw().getLocation());
            if (pt == null) {
                continue;
            }

            net.runelite.api.Point textLocation = Perspective.getCanvasTextLocation(
                    ctx.getClient(),
                    graphics,
                    pt,
                    sb.toString(),
                    20
            );

            if (textLocation != null) {
                OverlayUtil.renderTextLocation(graphics, textLocation, sb.toString(), textColor);
            }
        }
    }

    private String formatValue(int value) {
        if (value >= 1_000_000) {
            return String.format("%.1fM", value / 1_000_000.0);
        } else if (value >= 1_000) {
            return String.format("%.1fk", value / 1_000.0);
        }
        return String.valueOf(value);
    }
}
