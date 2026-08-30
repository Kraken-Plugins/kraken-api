package com.kraken.api.service.walker.transport;

import com.kraken.api.Context;
import com.kraken.api.query.widget.WidgetEntity;
import net.runelite.api.widgets.Widget;

/**
 * Clicks a widget when the dataset does not name a reliable menu action.
 *
 * <p>Hub buttons and fairy-ring dials sometimes have no {@code Travel} / {@code Teleport} action, or
 * expose a different verb. Preferred names are tried only when the widget actually offers them —
 * probing missing ones logs a resolver failure and, on a false match, can cancel an in-flight
 * click. The widget's own actions and a parameterless click remain the fallbacks.</p>
 */
public final class WidgetClicks {

    /** Menu actions destination and travel widgets are commonly known by. */
    private static final String[] PREFERRED_ACTIONS = {
            "Travel", "Teleport", "Fly", "Select", "Confirm", "Continue", "Yes", "Okay"
    };

    private WidgetClicks() {
    }

    /**
     * Clicks a widget, trying known actions and then a generic click.
     *
     * @param ctx the API context, used for the parameterless fallback
     * @param widget the widget to click, may be null
     * @return true when an interaction was dispatched
     */
    public static boolean click(Context ctx, WidgetEntity widget) {
        if (widget == null || widget.raw() == null) {
            return false;
        }

        Widget raw = widget.raw();
        for (String action : PREFERRED_ACTIONS) {
            if (hasAction(raw, action) && widget.interact(action)) {
                return true;
            }
        }

        if (raw != null && raw.getActions() != null) {
            for (String action : raw.getActions()) {
                if (action != null && !action.isEmpty() && widget.interact(action)) {
                    return true;
                }
            }
        }

        return raw != null && ctx.getInteractionManager().interact(raw, 0);
    }

    private static boolean hasAction(Widget raw, String wanted) {
        if (raw == null || raw.getActions() == null || wanted == null) {
            return false;
        }

        for (String action : raw.getActions()) {
            if (action != null && action.equalsIgnoreCase(wanted)) {
                return true;
            }
        }

        return false;
    }
}
