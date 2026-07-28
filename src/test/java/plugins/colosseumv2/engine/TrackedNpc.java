package plugins.colosseumv2.engine;

import lombok.Getter;
import lombok.Setter;
import plugins.colosseumv2.model.spawns.Mob;

/**
 * Mutable per-NPC engine state carried across ticks: observed attack history, line of sight
 * transitions, and the manticore charge/volley state machine.
 */
@Getter
@Setter
public class TrackedNpc {

    private final int index;
    private final int npcId;
    private final String name;
    private final Mob mob;

    private int sceneX;
    private int sceneY;
    private int size;

    private int firstSeenTick;

    /** Earliest tick this NPC may attack (wave-start delay or last attack + speed). */
    private int readyTick;

    private boolean losNow;
    private boolean losPrev;

    private boolean dead;

    /** Tick of the last observed attack animation, or -1. */
    private int lastAttackTick = -1;

    /** Observed attack count, used to anticipate the javelin colossus artillery cycle. */
    private int attackCount;

    /** Next predicted attack tick from the observed chain, or -1 when the chain is broken. */
    private int nextAttackTick = -1;

    // =====================
    // Manticore state
    // =====================

    /** Whether orb graphics have ever been observed on this manticore. */
    private boolean everCharged;

    /** Whether orb graphics were present on the previous tick. */
    private boolean hadOrbPrev;

    /** True when this NPC was present in the previous tick's snapshot (transition witnessing). */
    private boolean observedPrevTick;

    /** First orb style: TRUE = mage first, FALSE = range first, null = unknown. */
    private Boolean mageFirst;

    /** Tick charging was first observed, or -1. */
    private int chargeStartTick = -1;

    /** Plugin owns the next (first) volley because the charge was witnessed in line of sight. */
    private boolean witnessOwned;

    /** Predicted tick of the next volley's first orb, or -1 when unknown. */
    private int volleyReadyTick = -1;

    /** Tick the most recent volley started, or -1. */
    private int lastVolleyStartTick = -1;

    /** Whether the in-progress volley was plugin-predicted (entries for all three orbs). */
    private boolean currentVolleyPluginOwned;

    public TrackedNpc(int index, int npcId, String name, Mob mob) {
        this.index = index;
        this.npcId = npcId;
        this.name = name;
        this.mob = mob;
    }

    public boolean isManticore() {
        return mob != null && mob.isManticore();
    }

    public boolean isPrayable() {
        return mob != null && mob.isPrayable();
    }

    /**
     * Applies the current tick's snapshot position/size to this tracked state.
     */
    public void updateFromSnapshot(NpcSnapshot snapshot) {
        this.sceneX = snapshot.getSceneX();
        this.sceneY = snapshot.getSceneY();
        this.size = snapshot.getSize();
        this.dead = snapshot.isDead();
    }
}
