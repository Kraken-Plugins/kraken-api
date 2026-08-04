package plugins.api.requirements;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;

/**
 * An NPC that must be in the scene for a test to have something to act on.
 *
 * <p>This is a requirement the runner can only ever verify, never establish — NPCs cannot be
 * summoned. An unsatisfied NPC requirement is therefore an environment problem and produces a skip,
 * not a failure.</p>
 */
@Value
@Builder
public class NpcRequirement {

    /**
     * Candidate names, any one of which satisfies the requirement. The dps test, for example, is
     * happy with a Guard, a Man or a Goblin.
     */
    @Singular("anyOfName")
    List<String> anyOfNames;

    /**
     * Whether names match as a substring. True by default because the existing tests search with
     * {@code nameContains}, and NPC names carry suffixes such as level information.
     */
    @Builder.Default
    boolean nameContains = true;

    /** How close the NPC must be, in tiles. */
    @Builder.Default
    int withinTiles = 15;

    /** An optional menu action the NPC must offer, such as {@code "Bank"}. Null when unused. */
    String withAction;

    /**
     * A single named NPC within the default range.
     *
     * @param name the NPC name to look for
     * @return the requirement
     */
    public static NpcRequirement named(String name) {
        return builder().anyOfName(name).build();
    }

    /**
     * Any one of several named NPCs within the default range.
     *
     * @param names the acceptable NPC names
     * @return the requirement
     */
    public static NpcRequirement anyOf(String... names) {
        NpcRequirementBuilder builder = builder();
        for (String name : names) {
            builder.anyOfName(name);
        }
        return builder.build();
    }

    /**
     * A single named NPC within an explicit range.
     *
     * @param name the NPC name to look for
     * @param tiles how close the NPC must be
     * @return the requirement
     */
    public static NpcRequirement within(String name, int tiles) {
        return builder().anyOfName(name).withinTiles(tiles).build();
    }

    /**
     * Renders the requirement for a human, used verbatim in skip reasons.
     *
     * @return a short description such as {@code "a nearby Guard, Man or Goblin"}
     */
    public String describe() {
        String names = String.join(" or ", anyOfNames);
        String action = withAction == null ? "" : " offering '" + withAction + "'";
        return "a " + names + action + " within " + withinTiles + " tiles";
    }
}
