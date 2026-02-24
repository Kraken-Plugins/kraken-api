package plugins.colosseum.model;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import plugins.colosseum.ManticoreAttackStyle;
import plugins.colosseum.model.spawns.Mob;

@Data
@RequiredArgsConstructor
public class TrackedMobState {
    private final int npcIndex;
    private final Mob mob;

    private int lastSeenTick = -1;
    private boolean inLineOfSight;
    private boolean previousLineOfSight;

    private boolean synced;
    private int nextAttackTick = -1;
    private Integer knownAttackAnimation;
    private int lastAttackAnimationTick = -1;

    private boolean charging;
    private int chargeStartTick = -1;
    private int firstVolleyTick = -1;
    private boolean firstVolleyAuto;
    private boolean chargeInterrupted;
    private ManticoreAttackStyle firstManticoreStyle;
    private int lastManticoreSpotAnim = -1;
    private int activeVolleyTick = -1;
    private int nextVolleyTick = -1;
    private int lastQueuedTick = -1;
}
