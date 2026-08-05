package plugins.api.suite;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Broad category a test belongs to, mirroring the {@code plugins.api.tests} sub-packages.
 *
 * <p>Used to scope a run. A full pass takes a long time, so being able to run just the query group is
 * the difference between a three minute check and a half hour one.</p>
 */
@Getter
@AllArgsConstructor
public enum TestGroup {

    /** Runs first and always: verifies the harness can drive the client at all. */
    SELF_CHECK("Self check"),

    /** Fluent entity queries: npcs, players, objects, containers, widgets, worlds. */
    QUERY("Query"),

    /** Global game systems: bank, prayer, magic, dialogue, camera, movement, grand exchange. */
    SERVICE("Service"),

    /** Widget and menu action dispatch. */
    INTERACTION("Interaction"),

    /** Virtual mouse behaviour. */
    INPUT("Input");

    private final String displayName;
}
