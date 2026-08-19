package com.kraken.api.service.walker.transport;

import com.kraken.api.Context;
import com.kraken.api.query.npc.NpcEntity;
import com.kraken.api.query.tileobject.TileObjectEntity;
import lombok.extern.slf4j.Slf4j;
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
public final class TransportEntityResolver {

    /** How far from the transport's origin tile to look for the entity. */
    private static final int SEARCH_RADIUS = 4;

    private TransportEntityResolver() {
    }

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

        TileObjectEntity object = findObject(ctx, info, anchor);
        if (object != null && object.isPresent()) {
            ObjectInfo refined = info.withEntityName(object.getName());
            String chosen = action != null ? action
                    : chooseAction(refined, object.getObjectComposition() != null
                            ? object.getObjectComposition().getActions() : null);
            log.debug("Interacting with object {} using '{}'", refined.getId(), chosen);
            return object.interact(chosen);
        }

        NpcEntity npc = findNpc(ctx, info, anchor);
        if (npc != null && npc.isPresent()) {
            ObjectInfo refined = info.withEntityName(npc.getName());
            String chosen = action != null ? action : refined.getMenuOption();
            log.debug("Interacting with npc {} using '{}'", refined.getId(), chosen);
            return npc.interact(chosen);
        }

        log.debug("Could not find anything matching '{}' near {}", info, anchor);
        return false;
    }

    /**
     * Finds the scenery the transport refers to, preferring an exact id match and falling back to the
     * object's name when the id does not appear — a door reports a different id once it is open.
     */
    private static TileObjectEntity findObject(Context ctx, ObjectInfo info, WorldPoint anchor) {
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
                .near(anchor, SEARCH_RADIUS)
                .nearestTo(anchor);
        return byName != null && byName.isPresent() ? byName : null;
    }

    private static NpcEntity findNpc(Context ctx, ObjectInfo info, WorldPoint anchor) {
        if (info.hasId()) {
            NpcEntity byId = ctx.npcs().withId(info.getId()).nearestTo(anchor).first();
            if (byId != null && byId.isPresent()) {
                return byId;
            }
        }

        if (info.getMenuTarget().isEmpty()) {
            return null;
        }

        NpcEntity byName = ctx.npcs().withName(info.getMenuTarget()).nearestTo(anchor).first();
        return byName != null && byName.isPresent() ? byName : null;
    }

    /**
     * Picks the menu action to use.
     *
     * <p>The parsed option is preferred, but the split can still be wrong when the entity's name could
     * not be read. When the parsed option matches none of the entity's actions and exactly one of its
     * actions appears in the raw text, that action is used instead.</p>
     */
    private static String chooseAction(ObjectInfo info, String[] actions) {
        String option = info.getMenuOption();
        if (actions == null || actions.length == 0 || option.isEmpty()) {
            return option;
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

        return match != null ? match : option;
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
