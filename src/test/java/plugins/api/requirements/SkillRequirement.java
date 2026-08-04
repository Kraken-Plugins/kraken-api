package plugins.api.requirements;

import lombok.Builder;
import lombok.Value;
import net.runelite.api.Skill;

/**
 * A minimum skill level a test needs in order to perform its actions.
 */
@Value
@Builder
public class SkillRequirement {

    /** The skill being constrained. */
    Skill skill;

    /** The minimum level required. */
    int level;

    /**
     * Whether a temporary boost may be counted towards the requirement.
     *
     * <p>False by default, which additionally requires the boosted level to equal the real level —
     * that is, no boost is currently active. This is what the dps test means when it asks that no
     * stat be drifting: it compares readings taken seconds apart and expects gear to be the only
     * thing changing between them, so a potion wearing off mid run would look like a regression.</p>
     */
    @Builder.Default
    boolean allowBoosted = false;

    /**
     * A skill requirement that additionally forbids an active boost.
     *
     * @param skill the skill to constrain
     * @param level the minimum level required
     * @return the requirement
     */
    public static SkillRequirement of(Skill skill, int level) {
        return builder().skill(skill).level(level).build();
    }

    /**
     * A skill requirement that a temporary boost may satisfy.
     *
     * @param skill the skill to constrain
     * @param level the minimum level required
     * @return the requirement
     */
    public static SkillRequirement boostable(Skill skill, int level) {
        return builder().skill(skill).level(level).allowBoosted(true).build();
    }

    /**
     * Renders the requirement for a human, used verbatim in skip reasons.
     *
     * @return a short description such as {@code "40 ATTACK"}
     */
    public String describe() {
        return level + " " + skill.getName() + (allowBoosted ? " (boosts allowed)" : "");
    }
}
