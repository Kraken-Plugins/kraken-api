package com.kraken.api.service.walker.transport.registry;

import com.kraken.api.service.pathfinding.GlobalPathfinderConfig;
import com.kraken.api.service.walker.transport.TransportShape;
import lombok.Getter;

import java.util.function.Predicate;

/**
 * What kind of transport a definition is.
 *
 * <p>The category decides the planning option that turns a transport on and off: a search that
 * sets {@code useBoats(false)} skips every definition in {@link #BOAT}. {@link #GENERAL} has no
 * option and is always planned. The category also names the way such a transport is usually
 * operated, which a definition that states no shape takes.</p>
 */
public enum TransportCategory {

    /** Doors, gates, stairs, ladders and anything else no planning option covers. */
    GENERAL(TransportShape.SINGLE_CLICK, config -> true),

    /** Agility shortcuts, such as stepping stones, log balances and squeeze throughs. */
    AGILITY_SHORTCUT(TransportShape.SINGLE_CLICK, GlobalPathfinderConfig::isUseAgilityShortcuts),

    /** Shortcuts crossed with a crossbow and a mithril grapple. */
    GRAPPLE_SHORTCUT(TransportShape.SINGLE_CLICK, GlobalPathfinderConfig::isUseGrappleShortcuts),

    /** Rowboats and other small boats that carry the player along a fixed route. */
    BOAT(TransportShape.CLICK_THEN_DIALOGUE, GlobalPathfinderConfig::isUseBoats),

    /** Canoes shaped at a canoe station and paddled along the River Lum. */
    CANOE(TransportShape.CANOE, GlobalPathfinderConfig::isUseCanoes),

    /** Charter ships hired at a port to sail to another port. */
    CHARTER_SHIP(TransportShape.CLICK_THEN_DIALOGUE, GlobalPathfinderConfig::isUseCharterShips),

    /** Ships that sail between two ports, usually for a fare or after a quest. */
    SHIP(TransportShape.CLICK_THEN_DIALOGUE, GlobalPathfinderConfig::isUseShips),

    /** Fairy rings, travelled by dialling a three letter code. */
    FAIRY_RING(TransportShape.FAIRY_RING, GlobalPathfinderConfig::isUseFairyRings),

    /** Gnome gliders, flown by picking a destination on the glider map. */
    GNOME_GLIDER(TransportShape.CLICK_THEN_WIDGET, GlobalPathfinderConfig::isUseGnomeGliders),

    /** Hot air balloons, flown by picking a destination on the balloon map. */
    HOT_AIR_BALLOON(TransportShape.CLICK_THEN_WIDGET, GlobalPathfinderConfig::isUseHotAirBalloons),

    /** Magic carpets of the Kharidian Desert, hired from a carpet seller. */
    MAGIC_CARPET(TransportShape.CLICK_THEN_DIALOGUE, GlobalPathfinderConfig::isUseMagicCarpets),

    /** Magic mushtrees of Fossil Island, travelled by picking a destination tree. */
    MAGIC_MUSHTREE(TransportShape.CLICK_THEN_WIDGET, GlobalPathfinderConfig::isUseMagicMushtrees),

    /** Minecarts and train carts that run between fixed stations. */
    MINECART(TransportShape.HUB_RESUME_PAUSE, GlobalPathfinderConfig::isUseMinecarts),

    /** Quetzals flown from a landing site. A search weighs them against {@link #QUETZAL_WHISTLE}. */
    QUETZAL(TransportShape.CLICK_THEN_WIDGET, GlobalPathfinderConfig::isUseQuetzals),

    /**
     * The whistle that calls a quetzal from anywhere. It leads to the same places as
     * {@link #QUETZAL}, so a search only prefers it when it saves a real distance.
     */
    QUETZAL_WHISTLE(TransportShape.ITEM_SUBOP,
            config -> config.isUseQuetzals() && config.isUseTeleportationItems()),

    /** Teleports that only exist on seasonal worlds, such as a league's. */
    SEASONAL(TransportShape.ITEM_SUBOP,
            config -> config.isUseSeasonalTransports() && config.isUseTeleportationItems()),

    /** Spirit trees, travelled by picking a destination tree. */
    SPIRIT_TREE(TransportShape.HUB_RESUME_PAUSE, GlobalPathfinderConfig::isUseSpiritTrees),

    /** The jewellery box and the mounted teleport items of a player owned house. */
    TELEPORTATION_BOX(TransportShape.SINGLE_CLICK,
            config -> config.isUseTeleportationItems() && config.isUseTeleportationBoxes()),

    /** Teleports from an item that is worn or carried, such as jewellery, tablets and scrolls. */
    TELEPORTATION_ITEM(TransportShape.ITEM_SUBOP, GlobalPathfinderConfig::isUseTeleportationItems),

    /** Levers that teleport the player when pulled, such as the Wilderness levers. */
    TELEPORTATION_LEVER(TransportShape.SINGLE_CLICK, GlobalPathfinderConfig::isUseTeleportationLevers),

    /** Teleports from the grouping tab's minigame list. */
    TELEPORTATION_MINIGAME(TransportShape.GROUPING_TELEPORT, GlobalPathfinderConfig::isUseTeleportationMinigames),

    /** Portals and teleporters outside a player owned house that move the player when used. */
    TELEPORTATION_PORTAL(TransportShape.SINGLE_CLICK, GlobalPathfinderConfig::isUseTeleportationPortals),

    /** The portals of a player owned house: the portal chamber and the portal nexus. */
    TELEPORTATION_PORTAL_POH(TransportShape.SINGLE_CLICK, GlobalPathfinderConfig::isUseTeleportationPortalsPoh),

    /** Teleports cast from the spellbook. */
    TELEPORTATION_SPELL(TransportShape.SPELL, GlobalPathfinderConfig::isUseTeleportationSpells),

    /** The home teleport of each spellbook, which no planning option turns off. */
    TELEPORTATION_SPELL_HOME(TransportShape.SPELL, config -> true),

    /** Wilderness obelisks, which teleport everyone on their platform to another obelisk. */
    WILDERNESS_OBELISK(TransportShape.HUB_DIALOGUE, GlobalPathfinderConfig::isUseWildernessObelisks);

    /**
     * What a search adds to the cost of {@link #QUETZAL_WHISTLE} when it weighs the whistle against
     * a landing site, in tiles. The whistle spends a charge, so it has to save at least this much.
     */
    private static final int WHISTLE_DIFFERENTIAL_COST = 15;

    /** How a transport of this kind is operated when its definition states no shape. */
    @Getter
    private final TransportShape defaultShape;

    private final Predicate<GlobalPathfinderConfig> enabled;

    TransportCategory(TransportShape defaultShape, Predicate<GlobalPathfinderConfig> enabled) {
        this.defaultShape = defaultShape;
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

    /**
     * Reports whether this kind of transport leads to the same places as another kind. A search
     * lets such transports compete on cost instead of taking whichever it finds first.
     *
     * @return true for quetzals and the quetzal whistle
     */
    public boolean sharesDestinations() {
        return this == QUETZAL || this == QUETZAL_WHISTLE;
    }

    /**
     * Returns the cost a search adds to this kind of transport when it competes with another kind
     * for the same destination. It only decides which of the two is taken.
     *
     * @return the cost in tiles, zero for every kind but the quetzal whistle
     */
    public int getDifferentialCost() {
        return this == QUETZAL_WHISTLE ? WHISTLE_DIFFERENTIAL_COST : 0;
    }
}
