package com.kraken.api.service.walker;

import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * Clips a planned route to the part of it the client can actually click.
 *
 * <p>The scene is only 104 tiles square, so a cross-map route mostly refers to tiles that are not
 * loaded. Walking the part that is loaded, then re-planning once the scene has shifted, is what lets a
 * short-range movement primitive cover a long route.</p>
 *
 * <p>Pure arithmetic, so it is unit tested without a running client — everything around it in the walk
 * loop blocks on the game.</p>
 */
public final class SceneWindow {

    /** The scene is 104x104 tiles. */
    public static final int SCENE_SIZE = 104;

    /** Tiles trimmed from each scene edge; the outer ring is often unwalkable or unrendered. */
    public static final int DEFAULT_MARGIN = 8;

    private SceneWindow() {
    }

    /**
     * Takes the leading part of a route that lies within a scene window.
     *
     * <p>Truncation stops at the <em>first</em> point outside the window, even if later points come
     * back inside. A route that leaves and returns must not be stitched together, because the tiles in
     * between are not clickable and walking the later section would jump the player's click across the
     * gap.</p>
     *
     * @param route the planned waypoints, ordered from the player outwards; may be null
     * @param baseX world x of the scene's south west corner
     * @param baseY world y of the scene's south west corner
     * @param plane the plane the player is on
     * @param margin tiles to trim from each scene edge
     * @return the contiguous prefix of {@code route} inside the trimmed window, possibly empty
     */
    public static List<WorldPoint> clip(List<WorldPoint> route, int baseX, int baseY, int plane, int margin) {
        List<WorldPoint> leg = new ArrayList<>();
        if (route == null) {
            return leg;
        }

        int minX = baseX + margin;
        int minY = baseY + margin;
        int maxX = baseX + SCENE_SIZE - 1 - margin;
        int maxY = baseY + SCENE_SIZE - 1 - margin;

        for (WorldPoint point : route) {
            // A plane change means stairs or a ladder, which is a transport rather than a walk.
            if (point.getPlane() != plane) {
                break;
            }

            if (point.getX() < minX || point.getX() > maxX
                    || point.getY() < minY || point.getY() > maxY) {
                break;
            }

            leg.add(point);
        }

        return leg;
    }

    /**
     * Clips a route using the default scene margin.
     *
     * @param route the planned waypoints, ordered from the player outwards; may be null
     * @param baseX world x of the scene's south west corner
     * @param baseY world y of the scene's south west corner
     * @param plane the plane the player is on
     * @return the contiguous prefix of {@code route} inside the trimmed window, possibly empty
     */
    public static List<WorldPoint> clip(List<WorldPoint> route, int baseX, int baseY, int plane) {
        return clip(route, baseX, baseY, plane, DEFAULT_MARGIN);
    }

    /**
     * A clickable tile at the edge of the loaded scene, in the direction of a far-away target.
     *
     * <p>When the planned path starts outside the scene — a compressed route whose next tile is the
     * castle stairs, eighty tiles south — {@link #clip} is empty. Walking that edge tile is what
     * shifts the scene toward the rest of the route. Returning nothing from {@code clip} must not
     * be treated as "already there".</p>
     *
     * @param here where the player is standing, may be null
     * @param target where they are heading, may be on another plane; may be null
     * @param baseX world x of the scene's south west corner
     * @param baseY world y of the scene's south west corner
     * @param margin tiles to trim from each scene edge
     * @return an in-window tile toward {@code target} on the player's plane, or null when there is
     *         nowhere to walk (already on that edge, or a missing argument)
     */
    public static WorldPoint toward(WorldPoint here, WorldPoint target, int baseX, int baseY, int margin) {
        if (here == null || target == null) {
            return null;
        }

        int minX = baseX + margin;
        int minY = baseY + margin;
        int maxX = baseX + SCENE_SIZE - 1 - margin;
        int maxY = baseY + SCENE_SIZE - 1 - margin;

        WorldPoint edge = new WorldPoint(
                clamp(target.getX(), minX, maxX),
                clamp(target.getY(), minY, maxY),
                here.getPlane());

        return edge.equals(here) ? null : edge;
    }

    /**
     * A clickable tile at the edge of the loaded scene, using the default margin.
     *
     * @param here where the player is standing, may be null
     * @param target where they are heading, may be null
     * @param baseX world x of the scene's south west corner
     * @param baseY world y of the scene's south west corner
     * @return an in-window tile toward {@code target}, or null when there is nowhere to walk
     */
    public static WorldPoint toward(WorldPoint here, WorldPoint target, int baseX, int baseY) {
        return toward(here, target, baseX, baseY, DEFAULT_MARGIN);
    }

    private static int clamp(int value, int min, int max) {
        return Math.min(max, Math.max(min, value));
    }
}
