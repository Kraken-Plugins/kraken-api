package com.kraken.api.service.walker.transport;

import com.kraken.api.util.StringUtils;
import lombok.Getter;
import net.runelite.client.util.Text;

import java.util.Locale;
import java.util.Map;

/**
 * What a minigame teleport's display info says: the activity to pick on the grouping tab, and the
 * choice the game asks for afterwards when the activity has more than one landing.
 *
 * <p>The transport dataset writes {@code "Castle Wars Minigame Teleport"} for an activity with one
 * landing, and {@code "Rat Pits Minigame Teleport: 2. Varrock"} for one of several. A definition a
 * plugin registers can leave the {@code Minigame Teleport} suffix out and name the activity
 * alone.</p>
 */
@Getter
public final class GroupingTeleport {

    /** Returned as the activity id when the grouping tab lists no activity of that name. */
    public static final int NO_ACTIVITY = -1;

    /** What the dataset ends an activity's name with. */
    private static final String SUFFIX = "minigame teleport";

    /** What the action on the grouping tab's teleport button starts with once an activity is selected. */
    private static final String TELEPORT_TO = "Teleport to";

    /** The activity, as the grouping tab lists it. */
    private final String activity;

    /** The landing to choose after the teleport is clicked, or null when the activity has only one. */
    private final DisplayInfo choice;

    private GroupingTeleport(String activity, DisplayInfo choice) {
        this.activity = activity;
        this.choice = choice;
    }

    /**
     * Parses the display info of a minigame teleport.
     *
     * @param displayInfo the transport's display info, may be null or blank
     * @return the activity and the choice it carries, or null when it names no activity
     */
    public static GroupingTeleport parse(String displayInfo) {
        if (displayInfo == null) {
            return null;
        }

        String activity = displayInfo.trim();
        String choice = null;
        int separator = activity.indexOf(':');
        if (separator >= 0) {
            choice = activity.substring(separator + 1);
            activity = activity.substring(0, separator).trim();
        }

        if (activity.toLowerCase(Locale.ROOT).endsWith(SUFFIX)) {
            activity = activity.substring(0, activity.length() - SUFFIX.length()).trim();
        }

        return activity.isEmpty() ? null : new GroupingTeleport(activity, DisplayInfo.parse(choice));
    }

    /**
     * Finds the id the game gives an activity, which is what selects it on the grouping tab.
     *
     * <p>Names are compared without case, spacing or punctuation: the dataset writes
     * {@code Giant's Foundry} where the game writes {@code Giants' Foundry}.</p>
     *
     * @param activities the activities that have a teleport, by id, as the game lists them
     * @param activity the activity a transport names
     * @return the activity's id, or {@link #NO_ACTIVITY} when the game lists none of that name
     */
    public static int activityId(Map<Integer, String> activities, String activity) {
        String wanted = StringUtils.lettersAndDigits(activity);
        if (activities == null || wanted.isEmpty()) {
            return NO_ACTIVITY;
        }

        for (Map.Entry<Integer, String> entry : activities.entrySet()) {
            if (StringUtils.lettersAndDigits(entry.getValue()).equals(wanted)) {
                return entry.getKey();
            }
        }
        return NO_ACTIVITY;
    }

    /**
     * Finds the action that teleports to the selected activity among the actions of the grouping
     * tab's teleport button. With no activity selected the button only offers a bare
     * {@code Teleport}, which does nothing and is not returned.
     *
     * @param actions the button's actions, may be null or hold null entries
     * @return the action as the button spells it, or null when the button teleports nowhere
     */
    public static String teleportAction(String[] actions) {
        if (actions == null) {
            return null;
        }

        for (String action : actions) {
            if (action != null
                    && Text.removeTags(action).trim().regionMatches(true, 0, TELEPORT_TO, 0, TELEPORT_TO.length())) {
                return action;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "GroupingTeleport(activity=" + activity + ", choice=" + choice + ")";
    }
}
