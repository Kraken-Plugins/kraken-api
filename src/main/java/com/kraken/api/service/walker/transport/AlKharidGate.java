package com.kraken.api.service.walker.transport;

import net.runelite.api.Quest;
import net.runelite.api.gameval.ItemID;
import shortestpath.PrimitiveIntHashMap;
import shortestpath.WorldPointUtil;
import shortestpath.transport.Transport;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Overlay for the Al Kharid toll gate, which the shortest-path dataset treats as a free Open.
 *
 * <p>The pinned TSV lists four edges at {@code (3267/3268, 3227/3228, 0)} as {@code Open Gate 44050}
 * with no coins, quest or varp. The live objects are {@code 44598}/{@code 44599} and charge
 * {@code Pay-toll(10gp)} until Prince Ali Rescue is done. Without this overlay the planner walks
 * east of Lumbridge and the walker clicks {@code Open} on an id that is not in the scene.</p>
 *
 * <p>Usable when the gate is already free, or when the player can pay. Click strings are rewritten
 * to VitaLite's live action and ids so the plan log and the click agree. Delete this class if
 * shortest-path's TSV grows those requirements and ids.</p>
 */
public final class AlKharidGate {

    /** Varp that reaches 100 once the gate no longer charges. */
    public static final int GATE_VARP = 273;

    /** How many coins the unpaid gate takes. */
    public static final int TOLL = 10;

    /** Live southern gate (y=3227). The dataset still names 44050. */
    public static final int SOUTH_OBJECT_ID = 44598;

    /** Live northern gate (y=3228). */
    public static final int NORTH_OBJECT_ID = 44599;

    /** Menu action while the gate still charges. */
    public static final String PAY_TOLL = "Pay-toll(10gp)";

    private static final int NORTH_Y = 3228;

    private static final Transport[] NONE = new Transport[0];

    private static final Set<Integer> TILES = Set.of(
            WorldPointUtil.packWorldPoint(3267, 3227, 0),
            WorldPointUtil.packWorldPoint(3268, 3227, 0),
            WorldPointUtil.packWorldPoint(3267, 3228, 0),
            WorldPointUtil.packWorldPoint(3268, 3228, 0)
    );

    private AlKharidGate() {
    }

    /**
     * Packed origin and destination tiles the dataset uses for this gate.
     *
     * @return the four tiles, as packed world points
     */
    public static Set<Integer> tiles() {
        return TILES;
    }

    /**
     * Whether this transport is one of the four Al Kharid gate edges.
     *
     * @param transport the edge, may be null
     * @return true when both ends sit on the gate tiles
     */
    public static boolean matches(Transport transport) {
        if (transport == null) {
            return false;
        }
        return TILES.contains(transport.getOrigin()) && TILES.contains(transport.getDestination());
    }

    /**
     * Whether the player can currently cross: the gate is free, or they can pay the toll.
     *
     * @param coins coins carried
     * @param gateVarp current value of {@link #GATE_VARP}
     * @param princeAliFinished whether Prince Ali Rescue is complete
     * @return true when the edge should stay in the graph
     */
    public static boolean usable(int coins, int gateVarp, boolean princeAliFinished) {
        return isFree(gateVarp, princeAliFinished) || coins >= TOLL;
    }

    /**
     * Whether the gate no longer charges.
     *
     * @param gateVarp current value of {@link #GATE_VARP}
     * @param princeAliFinished whether Prince Ali Rescue is complete
     * @return true when the live menu is {@code Open} rather than {@link #PAY_TOLL}
     */
    public static boolean isFree(int gateVarp, boolean princeAliFinished) {
        return gateVarp >= 100 || princeAliFinished;
    }

    /**
     * Whether the player can currently cross, read from a requirements snapshot.
     *
     * @param state inventory, varps and completed quests
     * @return true when the edge should stay in the graph
     */
    public static boolean usable(TransportRequirements.PlayerState state) {
        return usable(coins(state), gateVarp(state), princeAliFinished(state));
    }

    /**
     * Why the unpaid gate cannot be used, or empty when it can.
     *
     * @param transport the edge about to be used, may be null
     * @param state the player state to check, may be null
     * @return a single reason, or empty
     */
    public static List<String> unmetReasons(Transport transport, TransportRequirements.PlayerState state) {
        if (!matches(transport) || state == null || usable(state)) {
            return List.of();
        }
        return List.of("needs " + TOLL + " coins to pay the Al Kharid gate");
    }

    /**
     * Removes the four gate edges from a packed transport map when they are not usable.
     *
     * <p>Other transports that happen to share an origin tile are left in place. {@code refresh()}
     * rebuilds the map on the next plan, so repeated strips do not accumulate.</p>
     *
     * @param packed origin to transport array, may be null
     */
    public static void strip(PrimitiveIntHashMap<Transport[]> packed) {
        if (packed == null) {
            return;
        }

        for (int origin : TILES) {
            Transport[] current = packed.get(origin);
            if (current == null || current.length == 0) {
                continue;
            }

            List<Transport> kept = new ArrayList<>(current.length);
            for (Transport transport : current) {
                if (!matches(transport)) {
                    kept.add(transport);
                }
            }

            packed.put(origin, kept.isEmpty() ? NONE : kept.toArray(NONE));
        }
    }

    /**
     * Drops the gate from both bank-aware packed maps when the player cannot cross.
     *
     * @param withoutBank packed transports before a bank visit
     * @param withBank packed transports after a bank visit
     * @param coins coins carried
     * @param gateVarp current value of {@link #GATE_VARP}
     * @param princeAliFinished whether Prince Ali Rescue is complete
     */
    public static void stripIfUnusable(PrimitiveIntHashMap<Transport[]> withoutBank,
                                      PrimitiveIntHashMap<Transport[]> withBank,
                                      int coins,
                                      int gateVarp,
                                      boolean princeAliFinished) {
        if (usable(coins, gateVarp, princeAliFinished)) {
            return;
        }
        strip(withoutBank);
        strip(withBank);
    }

    /**
     * Object info that matches the live gate, for the plan log and the click.
     *
     * <p>Unpaid is {@code Pay-toll(10gp)} on {@link #SOUTH_OBJECT_ID}/{@link #NORTH_OBJECT_ID}.
     * Free is {@code Open} on those same ids. Anything that is not this gate is returned unchanged.</p>
     *
     * @param transport the edge, may be null
     * @param free whether the gate no longer charges
     * @return the string to parse as object info, or null when {@code transport} is null
     */
    public static String liveObjectInfo(Transport transport, boolean free) {
        if (transport == null) {
            return null;
        }
        if (!matches(transport)) {
            return transport.getObjectInfo();
        }
        return liveObjectInfo(transport.getOrigin(), free);
    }

    /**
     * Object info that matches the live gate, read from a requirements snapshot.
     *
     * @param transport the edge, may be null
     * @param state inventory, varps and completed quests, may be null
     * @return the string to parse as object info, or null when {@code transport} is null
     */
    public static String liveObjectInfo(Transport transport, TransportRequirements.PlayerState state) {
        if (!matches(transport)) {
            return transport == null ? null : transport.getObjectInfo();
        }
        boolean free = state != null && isFree(gateVarp(state), princeAliFinished(state));
        return liveObjectInfo(transport.getOrigin(), free);
    }

    /**
     * Live object info for a packed origin on the gate tiles.
     *
     * @param packedOrigin a packed world point
     * @param free whether the gate no longer charges
     * @return {@code Open} or {@link #PAY_TOLL} on the live object id
     */
    public static String liveObjectInfo(int packedOrigin, boolean free) {
        int id = WorldPointUtil.unpackWorldY(packedOrigin) == NORTH_Y ? NORTH_OBJECT_ID : SOUTH_OBJECT_ID;
        String action = free ? "Open" : PAY_TOLL;
        return action + " Gate " + id;
    }

    private static int coins(TransportRequirements.PlayerState state) {
        Map<Integer, Integer> held = state.getItemQuantities();
        if (held == null) {
            return 0;
        }
        return held.getOrDefault(ItemID.COINS, 0);
    }

    private static int gateVarp(TransportRequirements.PlayerState state) {
        Map<Integer, Integer> varPlayers = state.getVarPlayerValues();
        if (varPlayers == null) {
            return 0;
        }
        return varPlayers.getOrDefault(GATE_VARP, 0);
    }

    private static boolean princeAliFinished(TransportRequirements.PlayerState state) {
        Set<Quest> completed = state.getCompletedQuests();
        return completed != null && completed.contains(Quest.PRINCE_ALI_RESCUE);
    }
}
