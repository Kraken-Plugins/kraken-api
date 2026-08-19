package com.kraken.api.service.walker.transport;

import net.runelite.api.coords.WorldPoint;
import shortestpath.WorldPointUtil;
import shortestpath.pathfinder.WildernessChecker;

/**
 * The wilderness bucket a tile falls into, matching the planner.
 *
 * <p>The search loop reports 0, 20, 30 or 31 — not the wiki's per-tile level — and transport
 * ceilings in the dataset are compared with {@code <=} against that bucket. Computing anything else
 * here, such as {@code (y - 3520) / 8} from the y coordinate alone, would refuse a teleport the
 * planner had already accepted: Lunar Isle and Wintertodt sit north of y 3520 and are not
 * wilderness at all.</p>
 */
public final class WildernessLevels {

    private WildernessLevels() {
    }

    /**
     * Returns the planner's wilderness bucket for a world tile.
     *
     * @param location the tile to classify, may be null
     * @return 0 outside the wilderness, 20 below level 20, 30 below level 30, or 31 in deep wilderness
     */
    public static int of(WorldPoint location) {
        if (location == null) {
            return 0;
        }

        return ofPacked(WorldPointUtil.packWorldPoint(location));
    }

    /**
     * Returns the planner's wilderness bucket for a packed world tile.
     *
     * <p>Most-specific area first, because the level-30 box sits inside the level-20 box, which sits
     * inside the wilderness box. {@link WildernessChecker} already carves out Ferox Enclave.</p>
     *
     * @param packed the tile packed the way {@link WorldPointUtil} packs it
     * @return 0, 20, 30 or 31
     */
    public static int ofPacked(int packed) {
        if (WildernessChecker.isInLevel30Wilderness(packed)) {
            return 31;
        }
        if (WildernessChecker.isInLevel20Wilderness(packed)) {
            return 30;
        }
        if (WildernessChecker.isInWilderness(packed)) {
            return 20;
        }
        return 0;
    }
}
