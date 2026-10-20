package com.kraken.api.service.map;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import net.runelite.api.WorldView;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;

/**
 * Checks which 64x64 map regions the client has loaded into the top-level scene, and whether that scene is an
 * instance. Inside an instance the loaded regions are those of the template the instance copies, which is how a
 * plugin recognizes a boss's instance.
 *
 * <p>Every check treats a missing world view, a missing region array and an empty one as no region loaded, which is
 * the client's state before the first scene loads and while it hops worlds.
 */
@Singleton
public class RegionService {
    private static final int[] NO_REGIONS = new int[0];

    private final Provider<Context> ctxProvider;

    /**
     * Creates the service.
     *
     * @param ctxProvider Provides the {@link Context} used to read the loaded scene on the client thread.
     */
    @Inject
    public RegionService(Provider<Context> ctxProvider) {
        this.ctxProvider = ctxProvider;
    }

    /**
     * Reads the map regions loaded into the top-level scene.
     * @return A copy of the loaded region ids in the client's order, empty when no scene is loaded.
     */
    public int[] getMapRegions() {
        Context ctx = ctxProvider.get();
        return ctx.runOnClientThread(() -> {
            WorldView worldView = ctx.getClient().getTopLevelWorldView();
            int[] regions = worldView == null ? null : worldView.getMapRegions();
            return regions == null ? NO_REGIONS : regions.clone();
        }, NO_REGIONS);
    }

    /**
     * Checks whether any loaded map region is one of the given regions.
     * @param regionIds The region ids to look for.
     * @return True when any loaded map region is one of {@code regionIds}, false otherwise or when no scene is loaded.
     */
    public boolean isInRegion(int... regionIds) {
        for (int region : getMapRegions()) {
            if (ArrayUtils.contains(regionIds, region)) return true;
        }
        return false;
    }

    /**
     * Checks whether the first loaded map region, the client's first entry in {@link #getMapRegions()}, is one of the
     * given regions.
     * @param regionIds The region ids to look for.
     * @return True when the first loaded map region is one of {@code regionIds}, false otherwise or when no scene is
     *         loaded.
     */
    public boolean isFirstMapRegion(int... regionIds) {
        int[] regions = getMapRegions();
        return regions.length > 0 && ArrayUtils.contains(regionIds, regions[0]);
    }

    /**
     * Checks whether the loaded map regions are exactly the given regions, in the same order.
     * @param regionIds The expected region ids, in the client's order.
     * @return True when the loaded map regions equal {@code regionIds}, false otherwise or when no scene is loaded.
     */
    public boolean mapRegionsEqual(int... regionIds) {
        int[] regions = getMapRegions();
        return regions.length > 0 && Arrays.equals(regions, regionIds);
    }

    /**
     * Checks whether the top-level scene is an instance, such as a boss room or raid.
     * @return True when the top-level scene is an instance, false otherwise or when no scene is loaded.
     */
    public boolean isInInstance() {
        Context ctx = ctxProvider.get();
        return ctx.runOnClientThread(() -> {
            WorldView worldView = ctx.getClient().getTopLevelWorldView();
            return worldView != null && worldView.isInstance();
        }, false);
    }
}
