package plugins.colosseumv2.engine;

import lombok.Builder;
import lombok.Value;
import plugins.colosseumv2.model.spawns.Mob;

/**
 * Immutable per-tick view of a colosseum NPC. Built from client state by the plugin so the
 * {@link PrayerEngine} stays free of client dependencies and remains unit testable.
 * Coordinates are scene coordinates of the NPC's southwest tile.
 */
@Value
@Builder(toBuilder = true)
public class NpcSnapshot {

    int index;
    int npcId;
    String name;

    /** Resolved mob profile, or {@code null} for tracked-but-unprofiled NPCs (warband, totem). */
    Mob mob;

    int sceneX;
    int sceneY;
    int size;

    /** Current animation id, or -1 when idle. */
    int animation;

    /** True when an animation change was observed for this NPC during the current tick. */
    boolean animationChanged;

    boolean hasMageOrb;
    boolean hasRangeOrb;
    boolean hasMeleeOrb;

    boolean dead;

    public boolean hasAnyOrb() {
        return hasMageOrb || hasRangeOrb || hasMeleeOrb;
    }
}
