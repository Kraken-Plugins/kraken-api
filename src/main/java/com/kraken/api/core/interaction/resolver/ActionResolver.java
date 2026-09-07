package com.kraken.api.core.interaction.resolver;

import com.kraken.api.core.interaction.model.MenuOption;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.runelite.client.util.Text;

import java.util.Optional;
import java.util.function.IntFunction;

/**
 * Stateless utility for matching action strings against available action arrays.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ActionResolver {

    /**
     * Finds the first action in {@code available} matching {@code requested},
     * then maps its index through {@code factory} to produce a MenuOption.
     * @param requested  The action string to match.
     * @param available  The array of actions to match.
     * @param factory    The factory to produce a MenuOption.
     * @return The first action in {@code available} matching {@code requested},
     */
    public static Optional<MenuOption> findAction(String requested, String[] available, IntFunction<MenuOption> factory) {
        if (available == null) return Optional.empty();

        for (int i = 0; i < available.length; i++) {
            if (matches(requested, available[i])) {
                return Optional.ofNullable(factory.apply(i));
            }
        }
        return Optional.empty();
    }

    /**
     * Compares a requested string with a candidate string to determine if they match as equal.
     * <p>
     * Both sides are stripped of markup, have non-breaking spaces normalised and are compared
     * case-insensitively. A caller may therefore ask for {@code "Make Sapphire necklace"} or for the
     * fully tagged {@code "Make <col=ff9040>Sapphire necklace</col>"} the game puts on the widget,
     * and either resolves. Stripping only the candidate made the colour tags part of the caller's
     * contract, which is not something a caller can reasonably know up front.
     *
     * @param requested The action being asked for, tagged or plain.
     *                  If {@code requested} is {@code null}, the method will return {@code false}.
     * @param candidate The action the entity offers, as the game wrote it.
     *                  If {@code candidate} is {@code null}, the method will return {@code false}.
     * @return {@code true} if the two name the same action; otherwise, {@code false}.
     */
    public static boolean matches(String requested, String candidate) {
        return requested != null && candidate != null
                && Text.standardize(requested).equals(Text.standardize(candidate));
    }

    /**
     * Reports whether an action names the "use the currently selected widget on this entity" interaction.
     * Callers ask for either the generic {@code "Use"} or {@code "Cast"}, the verb the game puts on a
     * selected spell, and both mean the same target selection.
     *
     * @param action The action being asked for. If {@code null}, the method returns {@code false}.
     * @return {@code true} if the action is a selected-widget target selection.
     */
    public static boolean isTargetSelection(String action) {
        return action != null && (action.equalsIgnoreCase("Use") || action.equalsIgnoreCase("Cast"));
    }
}
