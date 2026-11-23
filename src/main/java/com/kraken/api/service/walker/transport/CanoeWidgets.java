package com.kraken.api.service.walker.transport;

import com.kraken.api.util.StringUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.client.util.Text;

/**
 * Names the parts of the two interfaces a canoe is built and steered with, and picks the action to
 * send to them.
 *
 * <p>Shaping a trunk opens an interface with one layer per hull. The game gives a layer its
 * {@code Make} button only when the player's woodcutting level allows that hull. Paddling opens a
 * map of the river the station stands on, with one button per stop. A stop the canoe can reach
 * offers {@code Travel to} followed by its name, and a stop it cannot reach offers nothing, so the
 * action is both how a stop is recognised and the proof that this canoe gets there.</p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CanoeWidgets {

    /** The hull layers of the shaping interface, largest hull first. */
    private static final int[] HULL_LAYERS = {
            InterfaceID.Canoeing.WAKA,
            InterfaceID.Canoeing.STABLE_DUGOUT,
            InterfaceID.Canoeing.DUGOUT,
            InterfaceID.Canoeing.LOG
    };

    /** The layers that hold the stop buttons of the two river maps. */
    private static final int[] DESTINATION_LAYERS = {
            InterfaceID.CanoeMapLum.DESTINATIONS,
            InterfaceID.CanoeMapDougne.DESTINATIONS
    };

    /** What the action on a hull's button starts with. */
    private static final String MAKE = "Make";

    /** What the action on a reachable stop starts with. The stop's name follows it. */
    private static final String TRAVEL_TO = "Travel to";

    /**
     * Returns the layers of the shaping interface that hold a hull's button as their first child.
     *
     * @return the packed component ids, largest hull first
     */
    public static int[] hullLayers() {
        return HULL_LAYERS.clone();
    }

    /**
     * Returns the layers whose children are the stop buttons of a river map.
     *
     * @return the packed component ids, one per river
     */
    public static int[] destinationLayers() {
        return DESTINATION_LAYERS.clone();
    }

    /**
     * Finds the action that shapes a hull among the actions of a hull's button.
     *
     * @param actions the button's actions, may be null or hold null entries
     * @return the action as the button spells it, or null when the button does not shape a hull
     */
    public static String makeAction(String[] actions) {
        if (actions == null) {
            return null;
        }

        for (String action : actions) {
            if (action != null && startsWith(Text.removeTags(action), MAKE)) {
                return action;
            }
        }
        return null;
    }

    /**
     * Finds the action that paddles to a stop among the actions of a map button.
     *
     * <p>Names are compared without case, spacing or punctuation, because the transport dataset and
     * the game do not spell every stop the same way: the dataset has {@code The Clock Tower} where
     * the map has {@code The Clocktower}.</p>
     *
     * @param actions the button's actions, may be null or hold null entries
     * @param destination the stop to paddle to, as the transport names it
     * @return the action as the button spells it, or null when the button does not lead to that stop
     */
    public static String travelAction(String[] actions, String destination) {
        String wanted = StringUtils.lettersAndDigits(destination);
        if (actions == null || wanted.isEmpty()) {
            return null;
        }

        for (String action : actions) {
            if (action == null) {
                continue;
            }

            String plain = Text.removeTags(action).trim();
            if (startsWith(plain, TRAVEL_TO)
                    && StringUtils.lettersAndDigits(plain.substring(TRAVEL_TO.length())).equals(wanted)) {
                return action;
            }
        }
        return null;
    }

    private static boolean startsWith(String text, String prefix) {
        return text.trim().regionMatches(true, 0, prefix, 0, prefix.length());
    }
}
