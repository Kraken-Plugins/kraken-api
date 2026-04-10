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
     * The comparison is case-insensitive and applies sanitation to the candidate string
     * before performing the comparison.
     *
     * @param requested The string to be matched against.
     * @param candidate The string to check, potentially unsanitized.
     *                  If {@code candidate} is {@code null}, the method will return {@code false}.
     * @return {@code true} if the sanitized {@code candidate} matches {@code requested}
     *         (case-insensitively); otherwise, {@code false}.
     */
    public static boolean matches(String requested, String candidate) {
        return candidate != null && requested.equalsIgnoreCase(Text.sanitize(candidate));
    }
}
