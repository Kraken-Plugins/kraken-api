package plugins.colosseumv2.engine;

import lombok.Builder;
import lombok.Value;
import net.runelite.api.Prayer;
import plugins.colosseumv2.model.spawns.Mob;

/**
 * A single predicted incoming attack: the tick its damage will be rolled ({@link #landTick})
 * and the protection prayer that must already be active on that tick. The engine activates the
 * prayer one tick earlier, while exactly one tick remains.
 */
@Value
@Builder(toBuilder = true)
public class QueuedAttack {

    public enum Source {
        /** Predicted from an observed attack plus the mob's attack speed. */
        CHAIN,
        /** A manticore volley orb witnessed from charge start (plugin-owned first volley). */
        MANTICORE_WITNESSED,
        /** A manticore volley orb predicted from a previously observed volley. */
        MANTICORE_CHAIN,
        /** Predicted from jaguar pathing (arrival + cooldown). */
        JAGUAR_PATH,
        /** Entry retained after the source NPC died or despawned (config controlled). */
        GHOST
    }

    int npcIndex;
    String npcName;
    Mob mob;
    Prayer prayer;

    /** Absolute game tick on which the attack's damage is rolled. */
    int landTick;

    Source source;

    /** Largest hit this attack can deal, used to resolve same-tick prayer conflicts. */
    int conflictMaxHit;

    /** True when the jaguar path-priority option should let this entry win conflicts. */
    boolean jaguarPriority;

    /** True when this javelin attack is likely the prayer-ignoring artillery (every 5th). */
    boolean artilleryLikely;

    /** Short style label for overlays, e.g. "Mage", "Range", "Melee". */
    String styleLabel;
}
