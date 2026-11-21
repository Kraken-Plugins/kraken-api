package com.kraken.api.service.walker.transport.registry;

import com.kraken.api.service.pathfinding.GlobalPathfinderConfig;

import java.util.function.Predicate;

/**
 * What kind of transport a registered definition is, which decides the planning option that turns
 * it on and off.
 *
 * <p>A search that sets {@code useBoats(false)} skips every definition in {@link #BOAT}, the same
 * way it skips the dataset's boats. {@link #GENERAL} has no option and is always planned.</p>
 */
public enum TransportCategory {

    /** Doors, gates, stairs, ladders and anything else no planning option covers. */
    GENERAL(config -> true),

    AGILITY_SHORTCUT(GlobalPathfinderConfig::isUseAgilityShortcuts),
    GRAPPLE_SHORTCUT(GlobalPathfinderConfig::isUseGrappleShortcuts),
    BOAT(GlobalPathfinderConfig::isUseBoats),
    CANOE(GlobalPathfinderConfig::isUseCanoes),
    CHARTER_SHIP(GlobalPathfinderConfig::isUseCharterShips),
    SHIP(GlobalPathfinderConfig::isUseShips),
    FAIRY_RING(GlobalPathfinderConfig::isUseFairyRings),
    GNOME_GLIDER(GlobalPathfinderConfig::isUseGnomeGliders),
    HOT_AIR_BALLOON(GlobalPathfinderConfig::isUseHotAirBalloons),
    MAGIC_CARPET(GlobalPathfinderConfig::isUseMagicCarpets),
    MAGIC_MUSHTREE(GlobalPathfinderConfig::isUseMagicMushtrees),
    MINECART(GlobalPathfinderConfig::isUseMinecarts),
    QUETZAL(GlobalPathfinderConfig::isUseQuetzals),
    SEASONAL(GlobalPathfinderConfig::isUseSeasonalTransports),
    SPIRIT_TREE(GlobalPathfinderConfig::isUseSpiritTrees),
    TELEPORTATION_LEVER(GlobalPathfinderConfig::isUseTeleportationLevers),
    TELEPORTATION_PORTAL(GlobalPathfinderConfig::isUseTeleportationPortals),
    WILDERNESS_OBELISK(GlobalPathfinderConfig::isUseWildernessObelisks);

    private final Predicate<GlobalPathfinderConfig> enabled;

    TransportCategory(Predicate<GlobalPathfinderConfig> enabled) {
        this.enabled = enabled;
    }

    /**
     * Reports whether a search with these options may use transports of this kind.
     *
     * @param config the planning options of the search, or null for the defaults
     * @return true when the category is turned on
     */
    public boolean isEnabled(GlobalPathfinderConfig config) {
        return config == null || enabled.test(config);
    }
}
