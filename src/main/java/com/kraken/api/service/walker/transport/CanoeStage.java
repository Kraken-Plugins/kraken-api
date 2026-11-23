package com.kraken.api.service.walker.transport;

import java.util.Collection;

/**
 * How far along a canoe station is, read from the menu action it offers.
 *
 * <p>A canoe station is one object that changes form as the canoe is built. Each form offers a
 * single action, and that action is the only thing that says what to do next: fell the tree, shape
 * the trunk into a hull, push the hull into the water, then paddle it. The stage is kept per
 * player, so a station can already be part way along when the walker reaches it.</p>
 */
public enum CanoeStage {

    /** The tree still stands and has to be felled. */
    FELL("Chop-down"),

    /** The trunk is down and has to be shaped into a hull. */
    SHAPE("Shape-Canoe"),

    /** The hull is shaped and has to be pushed into the water: {@code Float Log}, {@code Float Canoe}. */
    FLOAT("Float"),

    /** The canoe is afloat and can be paddled: {@code Paddle Log}, {@code Paddle Canoe}. */
    PADDLE("Paddle"),

    /** The station offers nothing to do yet, as it does while the tree is falling. */
    BUSY(null);

    /** What the station's action starts with at this stage, or null when the stage has no action. */
    private final String actionPrefix;

    CanoeStage(String actionPrefix) {
        this.actionPrefix = actionPrefix;
    }

    /**
     * Reads the stage of a canoe station from the menu actions it offers.
     *
     * @param actions the station's menu actions, may be null or hold null entries
     * @return the stage whose action the station offers, or {@link #BUSY} when it offers none of them
     */
    public static CanoeStage of(Collection<String> actions) {
        for (CanoeStage stage : values()) {
            if (stage.actionIn(actions) != null) {
                return stage;
            }
        }
        return BUSY;
    }

    /**
     * Finds the menu action that moves a station on from this stage.
     *
     * @param actions the station's menu actions, may be null or hold null entries
     * @return the action as the station spells it, or null when the station is not at this stage
     */
    public String actionIn(Collection<String> actions) {
        if (actionPrefix == null || actions == null) {
            return null;
        }

        for (String action : actions) {
            if (action != null && action.regionMatches(true, 0, actionPrefix, 0, actionPrefix.length())) {
                return action;
            }
        }
        return null;
    }
}
