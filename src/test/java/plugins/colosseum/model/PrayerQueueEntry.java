package plugins.colosseum.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.Prayer;
import plugins.colosseum.model.spawns.Mob;

@Getter
@AllArgsConstructor
public class PrayerQueueEntry {
    private final int tick;
    private final int npcIndex;
    private final Mob mob;
    private final Prayer prayer;
    private final int maxHit;
    private final boolean jaguarPriority;
}
