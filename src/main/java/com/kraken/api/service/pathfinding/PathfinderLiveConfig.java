package com.kraken.api.service.pathfinding;

import com.kraken.api.Context;
import net.runelite.api.WorldType;

import java.util.EnumSet;

/**
 * Adjusts a pathfinder config for the world the player is actually on.
 *
 * <p>shortest-path has no members-world filter. On a free-to-play world the agility underwall,
 * canoes and jewellery the library would otherwise propose cannot be used, so those types are
 * turned off here. Boats, ships and standard-book spells stay on: Karamja and Varrock are
 * reachable without membership.</p>
 *
 * <p>A caller who already turned a type off keeps it off. This never turns a type back on.</p>
 */
public final class PathfinderLiveConfig {

    private PathfinderLiveConfig() {
    }

    /**
     * Applies live world restrictions to a requested config.
     *
     * @param config the caller's options, or null for library defaults
     * @param ctx used to read whether the current world is members
     * @return the config to pass to a search
     */
    public static GlobalPathfinderConfig resolve(GlobalPathfinderConfig config, Context ctx) {
        return resolve(config, isMembersWorld(ctx));
    }

    /**
     * Applies members-world restrictions without reading the client.
     *
     * @param config the caller's options, or null for library defaults
     * @param membersWorld whether the current world is a members world
     * @return the config to pass to a search
     */
    public static GlobalPathfinderConfig resolve(GlobalPathfinderConfig config, boolean membersWorld) {
        GlobalPathfinderConfig requested = config != null ? config : GlobalPathfinderConfig.builder().build();
        if (membersWorld) {
            return requested;
        }

        return requested.toBuilder()
                .useAgilityShortcuts(false)
                .useGrappleShortcuts(false)
                .useCanoes(false)
                .useCharterShips(false)
                .useFairyRings(false)
                .useGnomeGliders(false)
                .useHotAirBalloons(false)
                .useMagicCarpets(false)
                .useMagicMushtrees(false)
                .useMinecarts(false)
                .useQuetzals(false)
                .useSeasonalTransports(false)
                .useSpiritTrees(false)
                .useTeleportationBoxes(false)
                .useTeleportationItems(false)
                .useTeleportationMinigames(false)
                .useTeleportationPortals(false)
                .useTeleportationPortalsPoh(false)
                .useWildernessObelisks(false)
                .build();
    }

    /**
     * Whether the logged-in world is a members world.
     *
     * @param ctx used to read the client's world types
     * @return true on a members world, false on F2P or when the types cannot be read
     */
    public static boolean isMembersWorld(Context ctx) {
        if (ctx == null || ctx.getClient() == null) {
            return false;
        }

        Boolean members = ctx.runOnClientThread(() -> {
            EnumSet<WorldType> types = ctx.getClient().getWorldType();
            return types != null && types.contains(WorldType.MEMBERS);
        }, Boolean.FALSE);

        return Boolean.TRUE.equals(members);
    }
}
