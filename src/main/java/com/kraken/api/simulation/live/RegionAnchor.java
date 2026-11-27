package com.kraken.api.simulation.live;

import net.runelite.api.coords.WorldPoint;

/**
 * Anchors the simulation's 64x64 window on one game region's base: region local tiles are template world tiles
 * minus the base, the frame the simulation core and its traces use. Instanced arenas are anchored on instance
 * template points ({@code WorldPoint.fromLocalInstance}), whose region is the template's. Anchor once per encounter.
 */
public final class RegionAnchor {
    /** Anchors on whatever region the first point lies in. */
    public static final int ANY_REGION = -1;

    private final int regionId;
    private int baseX = -1;
    private int baseY = -1;
    private int plane;

    /** An anchor for any region. */
    public RegionAnchor() {
        this(ANY_REGION);
    }

    /**
     * An anchor that accepts only one region.
     * @param regionId the region id, or {@link #ANY_REGION}
     */
    public RegionAnchor(int regionId) {
        this.regionId = regionId;
    }

    /**
     * Anchors on the region of a template point.
     * @param templatePoint the point, usually the local player's server tile
     * @return true when anchored (false for null or a point outside the required region)
     */
    public boolean anchor(WorldPoint templatePoint) {
        if (templatePoint == null || (regionId != ANY_REGION && templatePoint.getRegionID() != regionId)) {
            return false;
        }
        baseX = templatePoint.getX() >> 6 << 6;
        baseY = templatePoint.getY() >> 6 << 6;
        plane = templatePoint.getPlane();
        return true;
    }

    /**
     * Whether the window is anchored.
     * @return true once {@link #anchor} succeeded
     */
    public boolean anchored() {
        return baseX >= 0;
    }

    /** Forgets the anchor. */
    public void reset() {
        baseX = -1;
        baseY = -1;
    }

    /**
     * The template world x of the window's west edge.
     * @return the base x, or -1 while not anchored
     */
    public int baseX() {
        return baseX;
    }

    /**
     * The template world y of the window's south edge.
     * @return the base y, or -1 while not anchored
     */
    public int baseY() {
        return baseY;
    }

    /**
     * The template point for a packed region local position (dispatch and overlays).
     * @param pos the packed position
     * @return the point on the anchored plane
     */
    public WorldPoint template(int pos) {
        return new WorldPoint(baseX + (pos & 0x3f), baseY + (pos >> 6 & 0x3f), plane);
    }

    /**
     * The packed region local position of a template point.
     * @param templatePoint the point
     * @param swOffset tiles to step south west first (a centre tile to its footprint's anchor), usually 0
     * @return the packed position, or -1 when outside the window
     */
    public int local(WorldPoint templatePoint, int swOffset) {
        int x = templatePoint.getX() - swOffset - baseX;
        int y = templatePoint.getY() - swOffset - baseY;
        if (x < 0 || y < 0 || x > 63 || y > 63) {
            return -1;
        }
        return x | y << 6;
    }
}
