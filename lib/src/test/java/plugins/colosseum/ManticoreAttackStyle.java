package plugins.colosseum;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.Prayer;

import java.util.List;

@Getter
@AllArgsConstructor
public enum ManticoreAttackStyle {
    RANGE(Prayer.PROTECT_FROM_MISSILES),
    MAGE(Prayer.PROTECT_FROM_MAGIC),
    MELEE(Prayer.PROTECT_FROM_MELEE);

    private final Prayer protectionPrayer;

    /**
     * Returns the manticore 3-hit sequence for the provided first style.
     *
     * @param firstStyle The style used for the first hit.
     * @return Ordered list of 3 styles for the volley.
     */
    public static List<ManticoreAttackStyle> sequenceForFirst(ManticoreAttackStyle firstStyle) {
        if (firstStyle == null) {
            return List.of();
        }

        switch (firstStyle) {
            case RANGE:
                return List.of(RANGE, MAGE, MELEE);
            case MAGE:
                return List.of(MAGE, RANGE, MELEE);
            case MELEE:
            default:
                return List.of(MELEE, RANGE, MAGE);
        }
    }
}
