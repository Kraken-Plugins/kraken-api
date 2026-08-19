package com.kraken.api.service.walker.transport;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.gameval.InterfaceID;
import shortestpath.transport.TransportType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Resolves a hub destination to the interface component that selects it.
 *
 * <p>Components are looked up from the client's own constants by name rather than copied here as
 * numbers, so the mapping follows the client version. What this file does hold is the handful of
 * places where the dataset and the client call a destination different things — a gnome glider stop
 * is {@code "Ta Quir Priw"} in the dataset and {@code GRANDTREE} in the interface.</p>
 */
@Slf4j
public final class HubWidgets {

    /** Returned when a destination has no matching component. */
    public static final int NOT_FOUND = -1;

    /** Gnome glider stops carry gnome names; the interface names them after where they land. */
    private static final Map<String, String> GLIDER_COMPONENTS = gliderComponents();

    /** Balloon destinations, whose interface buttons use shortened names. */
    private static final Map<String, String> BALLOON_COMPONENTS = balloonComponents();

    private HubWidgets() {
    }

    private static Map<String, String> gliderComponents() {
        Map<String, String> components = new HashMap<>();
        components.put(key("Ta Quir Priw"), "GRANDTREE_BUTTON");
        components.put(key("Sindarpos"), "WHITEWOLFMOUNTAIN_BUTTON");
        components.put(key("Lemanto Andra"), "VARROCK_BUTTON");
        components.put(key("Kar-Hewo"), "ALKHARID_BUTTON");
        components.put(key("Gandius"), "KARAMJA_BUTTON");
        components.put(key("Lemantolly Undri"), "OGREAREA_BUTTON");
        components.put(key("Ookookolly Undri"), "APEATOLL_BUTTON");
        return Collections.unmodifiableMap(components);
    }

    private static Map<String, String> balloonComponents() {
        Map<String, String> components = new HashMap<>();
        components.put(key("Castle Wars"), "BTN_CAST");
        components.put(key("Crafting Guild"), "BTN_CRAFT");
        components.put(key("Entrana"), "BTN_ENT");
        components.put(key("Grand Tree"), "BTN_GNO");
        components.put(key("Taverley"), "BTN_TAV");
        components.put(key("Varrock"), "BTN_VARR");
        return Collections.unmodifiableMap(components);
    }

    /**
     * Finds the component that selects a destination.
     *
     * @param type the kind of hub being used
     * @param displayInfo the parsed destination, may be null
     * @return the packed component id, or {@link #NOT_FOUND} when this hub is not selected this way
     */
    public static int component(TransportType type, DisplayInfo displayInfo) {
        if (type == null || displayInfo == null) {
            return NOT_FOUND;
        }

        switch (type) {
            case MAGIC_MUSHTREE:
                return positional(InterfaceID.FossilMushtrees.class, "BUTTON", displayInfo.getPosition());
            case GNOME_GLIDER:
                return named(InterfaceID.Glidermap.class, GLIDER_COMPONENTS.get(key(displayInfo.getLabel())));
            case HOT_AIR_BALLOON:
                return named(InterfaceID.ZepBalloonMap.class, BALLOON_COMPONENTS.get(key(displayInfo.getLabel())));
            default:
                return NOT_FOUND;
        }
    }

    /**
     * Returns the interface whose entries name their destinations, for hubs selected by reading the
     * list rather than by a fixed component.
     *
     * @param type the kind of hub being used
     * @return the interface group id, or {@link #NOT_FOUND}
     */
    public static int textInterfaceGroup(TransportType type) {
        if (type == null) {
            return NOT_FOUND;
        }

        switch (type) {
            case QUETZAL:
                return toGroup(InterfaceID.QuetzalMenu.CONTENTS);
            default:
                return NOT_FOUND;
        }
    }

    /**
     * Reports whether a hub is selected by clicking a component this class can name.
     *
     * @param type the kind of hub being used
     * @return true when {@link #component(TransportType, DisplayInfo)} can serve it
     */
    public static boolean hasFixedComponents(TransportType type) {
        return type == TransportType.MAGIC_MUSHTREE
                || type == TransportType.GNOME_GLIDER
                || type == TransportType.HOT_AIR_BALLOON;
    }

    private static int positional(Class<?> holder, String prefix, int position) {
        if (position < 1) {
            return NOT_FOUND;
        }
        return named(holder, prefix + position);
    }

    private static int named(Class<?> holder, String constant) {
        if (constant == null) {
            return NOT_FOUND;
        }

        try {
            return holder.getField(constant).getInt(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            log.debug("No component named {} on {}", constant, holder.getSimpleName());
            return NOT_FOUND;
        }
    }

    private static int toGroup(int packedId) {
        return packedId >>> 16;
    }

    /** Destination names are compared without case, spacing or punctuation. */
    private static String key(String value) {
        if (value == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder(value.length());
        for (char c : value.toLowerCase(Locale.ROOT).toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                builder.append(c);
            }
        }

        return builder.toString();
    }
}
