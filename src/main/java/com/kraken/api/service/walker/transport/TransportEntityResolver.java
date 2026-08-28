package com.kraken.api.service.walker.transport;

import com.kraken.api.Context;
import com.kraken.api.query.npc.NpcEntity;
import com.kraken.api.query.npc.NpcQuery;
import com.kraken.api.query.tileobject.TileObjectEntity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.coords.WorldPoint;

/**
 * Finds the thing a transport's object info refers to, and works out what to click on it.
 *
 * <p>The dataset stores a single id that may denote scenery or an NPC — {@code "Open Door 9398"} is a
 * wall object, {@code "Travel Holgart 7789"} is an NPC — so both are tried. Once the entity is found
 * its real name is used to split the menu option from the target, which is the only reliable way to
 * recover multi-word options such as {@code "Al Kharid"}.</p>
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TransportEntityResolver {

    /** How far from the transport's origin tile to look for scenery by id. */
    private static final int SEARCH_RADIUS = 4;

    /**
     * How far from the origin to look for scenery by name when the dataset id is stale or the origin
     * sits a few tiles off the object. The Tree Gnome Stronghold spirit tree is at {@code (2461, 3444)}
     * in VitaLite and the dataset origin is {@code (2461, 3449)} — five tiles, which four missed.
     */
    private static final int NAME_SEARCH_RADIUS = 10;

    /**
     * How far from the origin to look for an NPC. Sailors wander the dock; VitaLite uses ten tiles
     * for the same search, and four missed Captain Tobias standing a few tiles south of the origin.
     */
    private static final int NPC_SEARCH_RADIUS = 10;


    /**
     * Resolves the object info to a clickable entity and interacts with it.
     *
     * @param context the transport being crossed
     * @return true when an interaction was dispatched
     */
    public static boolean interact(TransportContext context) {
        return interactUsing(context, null);
    }

    /**
     * Resolves the object info to a clickable entity and interacts with it using a given action.
     *
     * <p>Used where the destination is chosen by the menu action rather than by a later interface —
     * a fairy ring's "Zanaris" option, for instance.</p>
     *
     * @param context the transport being crossed
     * @param action the menu action to use, or null to use the one the dataset names
     * @return true when an interaction was dispatched
     */
    public static boolean interactUsing(TransportContext context, String action) {
        ObjectInfo info = context.getObjectInfo();
        if (info == null) {
            log.debug("Transport at {} carries no object info", context.getOrigin());
            return false;
        }

        Context ctx = context.getCtx();
        WorldPoint anchor = context.getOrigin() != null ? context.getOrigin() : context.playerLocation();
        if (anchor == null) {
            return false;
        }

        TileObjectEntity object = findObject(ctx, info, anchor, context.playerLocation());
        if (object != null && object.isPresent()) {
            ObjectInfo refined = info.withEntityName(object.getName());
            String chosen = action != null ? action
                    : chooseAction(refined, object.getObjectComposition() != null
                            ? object.getObjectComposition().getActions() : null);
            if (chosen == null) {
                log.debug("No usable action on object {}", refined.getId());
                return false;
            }
            log.debug("Interacting with object {} using '{}'", refined.getId(), chosen);
            return object.interact(chosen);
        }

        NpcEntity npc = findNpc(ctx, info, anchor);
        if (npc != null && npc.isPresent()) {
            ObjectInfo refined = info.withEntityName(npc.getName());
            String chosen = action != null ? action : chooseAction(refined, npcActions(ctx, npc));
            if (chosen == null) {
                log.debug("No usable action on npc {}", refined.getId());
                return false;
            }
            log.debug("Interacting with npc {} using '{}'", refined.getId(), chosen);
            return npc.interact(chosen);
        }

        log.debug("Could not find anything matching '{}' near {}", info, anchor);
        return false;
    }

    /**
     * Finds the scenery the transport refers to, preferring an exact id match and falling back to the
     * object's name when the id does not appear — a door reports a different id once it is open.
     *
     * <p>Id search stays tight so two doors sharing an id are not confused. Name search is wider,
     * and the player's tile is tried when the origin itself is a few tiles off the scenery.</p>
     * @param ctx the API context
     * @param info the parsed object info
     * @param anchor the transport origin, used first
     * @param playerLocation the player's tile, used when the origin search misses
     * @return the scenery, or null when nothing matching is nearby
     */
    private static TileObjectEntity findObject(Context ctx, ObjectInfo info, WorldPoint anchor,
                                               WorldPoint playerLocation) {
        TileObjectEntity found = findObjectNear(ctx, info, anchor);
        if (found != null) {
            return found;
        }

        if (playerLocation != null && (anchor == null || playerLocation.distanceTo(anchor) > 0)) {
            return findObjectNear(ctx, info, playerLocation);
        }

        return null;
    }

    private static TileObjectEntity findObjectNear(Context ctx, ObjectInfo info, WorldPoint anchor) {
        if (anchor == null) {
            return null;
        }

        if (info.hasId()) {
            TileObjectEntity byId = ctx.tileObjects()
                    .withId(info.getId())
                    .near(anchor, SEARCH_RADIUS)
                    .nearestTo(anchor);
            if (byId != null && byId.isPresent()) {
                return byId;
            }
        }

        if (info.getMenuTarget().isEmpty()) {
            return null;
        }

        TileObjectEntity byName = ctx.tileObjects()
                .withName(info.getMenuTarget())
                .near(anchor, NAME_SEARCH_RADIUS)
                .nearestTo(anchor);
        return byName != null && byName.isPresent() ? byName : null;
    }

    /**
     * Finds the NPC the transport refers to. The dataset id is tried first; when it is stale — Captain
     * Tobias is {@code 14978} on the dock and {@code 14979} in the TSV — the remainder is matched as a
     * name suffix so {@code "Musa Point Captain Tobias"} still finds him.
     */
    private static NpcEntity findNpc(Context ctx, ObjectInfo info, WorldPoint anchor) {
        if (info.hasId()) {
            NpcEntity byId = nearbyNpcs(ctx, anchor).withId(info.getId()).nearestTo(anchor).first();
            if (byId != null && byId.isPresent()) {
                return byId;
            }
        }

        NpcEntity byName = nearbyNpcs(ctx, anchor)
                .filter(npc -> info.namesEntity(npc.getName()))
                .nearestTo(anchor)
                .first();
        return byName != null && byName.isPresent() ? byName : null;
    }

    private static NpcQuery nearbyNpcs(Context ctx, WorldPoint anchor) {
        return ctx.npcs().filter(npc -> withinRadius(npc.raw().getWorldLocation(), anchor, NPC_SEARCH_RADIUS));
    }

    private static boolean withinRadius(WorldPoint location, WorldPoint anchor, int radius) {
        return location != null && location.distanceTo(anchor) <= radius;
    }

    /**
     * Reads the NPC's live menu actions on the client thread.
     *
     * <p>Must use the transformed composition, the same source {@code NpcMenuActionResolver} uses
     * when clicking. The untransformed definition can still list {@code Musa Point} on a sailor
     * whose live menu is only {@code Travel}; picking from the definition then fails at click time.</p>
     */
    private static String[] npcActions(Context ctx, NpcEntity npc) {
        return ctx.runOnClientThread(() -> {
            NPC raw = npc.raw();
            if (raw == null) {
                return null;
            }
            NPCComposition composition = raw.getTransformedComposition();
            if (composition == null) {
                composition = raw.getComposition();
            }
            return composition != null ? composition.getActions() : null;
        }, null);
    }

    /**
     * Picks the menu action to use.
     *
     * <p>The parsed option is preferred, but the split can still be wrong when the entity's name could
     * not be read. When the parsed option matches none of the entity's actions and exactly one of its
     * actions appears in the raw text, that action is used instead. An empty option — a row such as
     * {@code "Fairy ring 29560"} — falls back to {@code Configure}, {@code Travel} or a climb action
     * on the entity, then to the first real action it offers. A named option the entity does not
     * have — {@code "Musa Point"} on the older Travel sailor — falls back to {@code Travel} or
     * {@code Pay-fare} rather than sending a string the client will reject. {@code "Open"} on the
     * unpaid Al Kharid gate falls back to {@code Pay-toll(10gp)} the same way.</p>
     *
     * @param info the parsed object info
     * @param actions the entity's menu actions, may be null
     * @return the action to send, or null when nothing usable was found
     */
    public static String chooseAction(ObjectInfo info, String[] actions) {
        String option = info.getMenuOption();
        if (actions == null || actions.length == 0) {
            return option.isEmpty() ? null : option;
        }

        if (option.isEmpty()) {
            return fallbackAction(actions);
        }

        // The dataset and the client do not always agree on punctuation: the dataset records
        // "Climb Down Ladder" where the client's action is "Climb-down". Comparing without
        // separators keeps those rows working, and the client's spelling is what gets sent.
        String wanted = normalise(option);
        for (String action : actions) {
            if (action != null && normalise(action).equals(wanted)) {
                return action;
            }
        }

        String remainder = normalise(info.getRemainder());
        String match = null;
        for (String action : actions) {
            if (action == null || action.isEmpty()) {
                continue;
            }
            if (remainder.startsWith(normalise(action))) {
                if (match != null) {
                    return option;
                }
                match = action;
            }
        }

        if (match != null) {
            return match;
        }

        String travel = firstMatchingAction(actions, TRAVEL_FALLBACKS);
        if (travel != null) {
            return travel;
        }
        String crossing = firstMatchingAction(actions, CROSSING_FALLBACKS);
        return crossing != null ? crossing : option;
    }

    /** Actions tried when the dataset named no option of its own, in preference order. */
    private static final String[] EMPTY_OPTION_PREFERENCES = {
            "Configure", "Travel", "Climb-up", "Climb-down", "Climb"
    };

    /**
     * Live sailor actions used when the dataset names a destination the NPC does not offer.
     * {@code Musa Point} is on the post-Sailing Locations NPC; the F2P dock still has {@code Travel}.
     */
    private static final String[] TRAVEL_FALLBACKS = {
            "Travel", "Pay-fare", "Travel-boat", "Take-boat"
    };

    /**
     * Live gate actions used when the dataset names {@code Open} on a toll gate that no longer
     * offers it. {@code Pay-toll(10gp)} is what Al Kharid shows until Prince Ali Rescue is done.
     */
    private static final String[] CROSSING_FALLBACKS = {
            "Pay-toll(10gp)", "Pay-toll"
    };

    private static String fallbackAction(String[] actions) {
        String preferred = firstMatchingAction(actions, EMPTY_OPTION_PREFERENCES);
        if (preferred != null) {
            return preferred;
        }

        for (String action : actions) {
            if (action != null && !action.isEmpty()) {
                return action;
            }
        }

        return null;
    }

    private static String firstMatchingAction(String[] actions, String[] preferred) {
        for (String want : preferred) {
            String wanted = normalise(want);
            for (String action : actions) {
                if (action != null && normalise(action).equals(wanted)) {
                    return action;
                }
            }
        }
        return null;
    }

    /** Compares menu text ignoring case and the hyphens and spaces that separate its words. */
    private static String normalise(String value) {
        if (value == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder(value.length());
        for (char c : value.toCharArray()) {
            if (c != '-' && c != ' ' && c != '_') {
                builder.append(Character.toLowerCase(c));
            }
        }

        return builder.toString();
    }
}
