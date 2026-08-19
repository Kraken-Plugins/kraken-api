package com.kraken.api.service.walker.transport;

import shortestpath.transport.TransportType;

import java.util.EnumMap;
import java.util.Map;

/**
 * Maps each kind of transport to the way it is operated.
 *
 * <p>Kept pure and separate from handler wiring so that the mapping can be checked for completeness
 * without a client. If the pinned dataset gains a transport type, the exhaustiveness test fails and
 * the gap is caught at build time rather than mid-walk.</p>
 */
public final class TransportShapes {

    private static final Map<TransportType, TransportShape> SHAPES = build();

    private TransportShapes() {
    }

    private static Map<TransportType, TransportShape> build() {
        Map<TransportType, TransportShape> shapes = new EnumMap<>(TransportType.class);

        shapes.put(TransportType.TRANSPORT, TransportShape.SINGLE_CLICK);
        shapes.put(TransportType.AGILITY_SHORTCUT, TransportShape.SINGLE_CLICK);
        shapes.put(TransportType.GRAPPLE_SHORTCUT, TransportShape.SINGLE_CLICK);
        shapes.put(TransportType.TELEPORTATION_LEVER, TransportShape.SINGLE_CLICK);
        shapes.put(TransportType.TELEPORTATION_PORTAL, TransportShape.SINGLE_CLICK);
        shapes.put(TransportType.TELEPORTATION_PORTAL_POH, TransportShape.SINGLE_CLICK);
        shapes.put(TransportType.TELEPORTATION_BOX, TransportShape.SINGLE_CLICK);

        // Ships look like a single menu click in the dataset ("Musa Point" on Captain Tobias),
        // but the live NPC is often the older Travel sailor, which then asks Yes please. Clicking
        // and then walking the conversation covers both variants.
        shapes.put(TransportType.SHIP, TransportShape.CLICK_THEN_DIALOGUE);
        shapes.put(TransportType.BOAT, TransportShape.CLICK_THEN_DIALOGUE);
        shapes.put(TransportType.MAGIC_CARPET, TransportShape.CLICK_THEN_DIALOGUE);
        shapes.put(TransportType.CHARTER_SHIP, TransportShape.CLICK_THEN_DIALOGUE);

        // Hub transports store their stops as nodes and are expanded into every permutation, so the
        // first click only opens a chooser. Which destination to pick comes from the expanded edge's
        // display info; what differs between them is the interface that does the choosing.
        shapes.put(TransportType.FAIRY_RING, TransportShape.FAIRY_RING);
        shapes.put(TransportType.SPIRIT_TREE, TransportShape.HUB_RESUME_PAUSE);
        shapes.put(TransportType.MAGIC_MUSHTREE, TransportShape.CLICK_THEN_WIDGET);
        shapes.put(TransportType.GNOME_GLIDER, TransportShape.CLICK_THEN_WIDGET);
        shapes.put(TransportType.HOT_AIR_BALLOON, TransportShape.CLICK_THEN_WIDGET);
        shapes.put(TransportType.QUETZAL, TransportShape.CLICK_THEN_WIDGET);
        shapes.put(TransportType.MINECART, TransportShape.HUB_RESUME_PAUSE);
        shapes.put(TransportType.CANOE, TransportShape.CANOE);
        shapes.put(TransportType.WILDERNESS_OBELISK, TransportShape.HUB_DIALOGUE);

        shapes.put(TransportType.TELEPORTATION_ITEM, TransportShape.ITEM_SUBOP);
        shapes.put(TransportType.QUETZAL_WHISTLE, TransportShape.ITEM_SUBOP);
        shapes.put(TransportType.SEASONAL_TRANSPORTS, TransportShape.ITEM_SUBOP);

        shapes.put(TransportType.TELEPORTATION_SPELL, TransportShape.SPELL);
        shapes.put(TransportType.TELEPORTATION_SPELL_HOME, TransportShape.SPELL);

        shapes.put(TransportType.TELEPORTATION_MINIGAME, TransportShape.GROUPING_TELEPORT);

        return shapes;
    }

    /**
     * Returns how a kind of transport is operated.
     *
     * @param type the kind of transport, may be null
     * @return the shape, or null when the type is unknown to this mapping
     */
    public static TransportShape of(TransportType type) {
        return type != null ? SHAPES.get(type) : null;
    }

    /**
     * Reports whether a kind of transport has a known shape.
     *
     * @param type the kind of transport
     * @return true when the mapping covers it
     */
    public static boolean isMapped(TransportType type) {
        return of(type) != null;
    }
}
