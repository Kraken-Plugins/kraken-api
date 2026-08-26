package com.kraken.api.service.walker.transport;

import com.kraken.api.Context;
import com.kraken.api.query.widget.WidgetEntity;
import net.runelite.api.widgets.Widget;

import java.util.Locale;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Dismisses a warning interface that some transports open after the first click.
 *
 * <p>Entrana boats put a {@code WARNING} title up rather than a chat option. Crossing the wilderness
 * ditch puts up {@code Wilderness Warning} with {@code Enter Wilderness}. The overlay often appears
 * a tick after the object click, so this is polled while waiting for arrival rather than only once
 * immediately after interacting.</p>
 *
 * <p>The spellbook always offers a {@code Warnings} filter. Matching that as a warning cancelled
 * staircase climbs — the walker clicked the magic tab instead of waiting for the climb. Bare
 * {@code WARNING} is still matched exactly; overlay titles are a separate check that requires the
 * word {@code warning} without accepting the filter.</p>
 */
public final class WarningWidgets {

    /** Actions that dismiss a warning overlay, in preference order. */
    private static final String[] DISMISS_ACTIONS = {
            "Continue", "Yes", "Enter Wilderness", "Proceed regardless"
    };

    /** Option index VitaLite sends to resume-pause on the overlay's universe parent. */
    private static final int RESUME_OPTION = 1;

    /** The word warning as its own token, so {@code Warnings} does not match. */
    private static final Pattern WARNING_WORD = Pattern.compile("(?i)\\bwarning\\b");

    private WarningWidgets() {
    }

    /**
     * Whether widget text is a warning title, not a settings filter named Warnings.
     *
     * @param text the widget's text, may be null or colour-tagged
     * @return true when it is the word WARNING, optionally with trailing punctuation
     */
    public static boolean isWarningTitle(String text) {
        String letters = lettersOnly(visibleText(text));
        return letters != null && letters.equalsIgnoreCase("WARNING");
    }

    /**
     * Whether widget text is a transport warning overlay, including titled ones such as Wilderness
     * Warning.
     *
     * @param text the widget's text, may be null or colour-tagged
     * @return true when the visible text contains the word warning, but is not the spellbook filter
     */
    public static boolean isOverlayTitle(String text) {
        String visible = visibleText(text);
        if (visible == null) {
            return false;
        }

        String letters = lettersOnly(visible);
        if (letters != null && letters.equalsIgnoreCase("Warnings")) {
            return false;
        }

        return isWarningTitle(text) || WARNING_WORD.matcher(visible).find();
    }

    /**
     * Whether an interface name is the overlay container VitaLite resume-pauses on.
     *
     * @param name a widget or InterfaceID name, may be null
     * @return true when the name contains {@code universe}
     */
    public static boolean isUniverseInterface(String name) {
        return name != null && name.toLowerCase(Locale.ROOT).contains("universe");
    }

    /**
     * Clicks through a warning widget if one is showing.
     *
     * @param ctx the API context
     * @return true when a warning was found and a click was dispatched
     */
    public static boolean dismiss(Context ctx) {
        if (ctx == null) {
            return false;
        }

        WidgetEntity title = findTitle(ctx, widget -> isWarningTitle(widget.getText()));
        if (title == null) {
            title = findTitle(ctx, widget -> isOverlayTitle(widget.getText()));
        }
        if (title == null) {
            return false;
        }

        int group = title.getId() >>> 16;
        if (clickNamedButton(ctx, group)) {
            return true;
        }

        Widget rawTitle = title.raw();
        int packedId = ctx.runOnClientThread(() -> packedResumeId(rawTitle), -1);
        return packedId != -1 && ctx.getInteractionManager().selectDialogueOption(packedId, RESUME_OPTION);
    }

    private static WidgetEntity findTitle(Context ctx, Predicate<Widget> match) {
        WidgetEntity title = ctx.widgets().visible().filter(widget -> {
            Widget raw = widget.raw();
            return raw != null && match.test(raw);
        }).first();
        return title != null && title.isPresent() ? title : null;
    }

    private static boolean clickNamedButton(Context ctx, int group) {
        for (String action : DISMISS_ACTIONS) {
            WidgetEntity button = ctx.widgets().inGroup(group).visible().withAction(action).first();
            if (button != null && button.isPresent() && button.interact(action)) {
                return true;
            }

            WidgetEntity labelled = ctx.widgets().inGroup(group).visible().withText(action, true).first();
            if (labelled == null || !labelled.isPresent()) {
                continue;
            }
            if (labelled.interact(action)) {
                return true;
            }
            if (ctx.getInteractionManager().selectDialogueOption(labelled.getId(), RESUME_OPTION)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Packed id to resume-pause on. {@link Widget#getParent()} must run on the client thread.
     *
     * @param title the overlay title widget, may be null
     * @return the parent's packed id, or the title's id, or -1
     */
    private static int packedResumeId(Widget title) {
        if (title == null) {
            return -1;
        }

        Widget universe = universeParent(title);
        if (universe != null) {
            return universe.getId();
        }

        Widget parent = title.getParent();
        return parent != null ? parent.getId() : title.getId();
    }

    private static Widget universeParent(Widget title) {
        Widget current = title.getParent();
        while (current != null) {
            if (isUniverseInterface(current.getName())) {
                return current;
            }
            current = current.getParent();
        }
        return null;
    }

    private static String visibleText(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }

        String clean = text.replaceAll("<[^>]*>", "").trim();
        return clean.isEmpty() ? null : clean;
    }

    private static String lettersOnly(String visible) {
        if (visible == null) {
            return null;
        }

        int end = visible.length();
        while (end > 0 && !Character.isLetter(visible.charAt(end - 1))) {
            end--;
        }
        return end == 0 ? null : visible.substring(0, end);
    }
}
